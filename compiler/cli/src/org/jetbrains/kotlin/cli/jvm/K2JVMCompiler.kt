/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.jvm.repl.ReplFromTerminal
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.TimeUnit

class K2JVMCompiler : CLICompiler<K2JVMCompilerArguments>() {
    override fun doExecute(
        arguments: K2JVMCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        PerformanceCounter.setTimeCounterEnabled(arguments.reportPerf)

        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        configureJdkHome(arguments, configuration, messageCollector).let {
            if (it != OK) return it
        }

        val pluginLoadResult = PluginCliParser.loadPluginsSafe(arguments, configuration)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        if (!arguments.script && arguments.buildFile == null) {
            for (arg in arguments.freeArgs) {
                val file = File(arg)
                if (file.extension == JavaFileType.DEFAULT_EXTENSION) {
                    configuration.addJavaSourceRoot(file)
                } else {
                    configuration.addKotlinSourceRoot(arg)
                    if (file.isDirectory) {
                        configuration.addJavaSourceRoot(file)
                    }
                }
            }
        }

        configuration.put(CommonConfigurationKeys.MODULE_NAME, arguments.moduleName ?: JvmAbi.DEFAULT_MODULE_NAME)

        if (arguments.buildFile == null) {
            configureContentRoots(paths, arguments, configuration)

            if (arguments.freeArgs.isEmpty() && !arguments.version) {
                if (arguments.script) {
                    messageCollector.report(ERROR, "Specify script source path to evaluate")
                    return COMPILATION_ERROR
                }
                ReplFromTerminal.run(rootDisposable, configuration)
                return ExitCode.OK
            }
        }

        if (arguments.includeRuntime) {
            configuration.put(JVMConfigurationKeys.INCLUDE_RUNTIME, true)
        }
        val friendPaths = arguments.friendPaths?.toList()
        if (friendPaths != null) {
            configuration.put(JVMConfigurationKeys.FRIEND_PATHS, friendPaths)
        }

        if (arguments.jvmTarget != null) {
            val jvmTarget = JvmTarget.fromString(arguments.jvmTarget!!)
            if (jvmTarget != null) {
                configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
            } else {
                messageCollector.report(
                    ERROR, "Unknown JVM target version: ${arguments.jvmTarget}\n" +
                            "Supported versions: ${JvmTarget.values().joinToString { it.description }}"
                )
            }
        }

        configuration.put(JVMConfigurationKeys.PARAMETERS_METADATA, arguments.javaParameters)

        putAdvancedOptions(configuration, arguments)

        messageCollector.report(LOGGING, "Configuring the compilation environment")
        try {
            val destination = arguments.destination

            if (arguments.buildFile != null) {
                if (destination != null) {
                    messageCollector.report(
                        STRONG_WARNING,
                        "The '-d' option with a directory destination is ignored because '-Xbuild-file' is specified"
                    )
                }

                val sanitizedCollector = FilteringMessageCollector(messageCollector, VERBOSE::contains)
                val buildFile = File(arguments.buildFile)
                val moduleChunk = CompileEnvironmentUtil.loadModuleChunk(buildFile, sanitizedCollector)

                configuration.put(JVMConfigurationKeys.MODULE_XML_FILE, buildFile)

                KotlinToJVMBytecodeCompiler.configureSourceRoots(configuration, moduleChunk.modules, buildFile)

                val environment = createCoreEnvironment(rootDisposable, configuration, arguments, messageCollector)
                        ?: return COMPILATION_ERROR

                registerJavacIfNeeded(environment, arguments).let {
                    if (!it) return COMPILATION_ERROR
                }

                KotlinToJVMBytecodeCompiler.compileModules(environment, buildFile, moduleChunk.modules)
            } else if (arguments.script) {
                val sourcePath = arguments.freeArgs.first()
                configuration.addKotlinSourceRoot(sourcePath)

                configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

                val environment = createCoreEnvironment(rootDisposable, configuration, arguments, messageCollector)
                        ?: return COMPILATION_ERROR

                val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(environment.project)
                val scriptFile = File(sourcePath)
                if (scriptFile.isDirectory || !scriptDefinitionProvider.isScript(scriptFile.name)) {
                    val extensionHint =
                        if (configuration.get(JVMConfigurationKeys.SCRIPT_DEFINITIONS) == listOf(StandardScriptDefinition)) " (.kts)"
                        else ""
                    messageCollector.report(ERROR, "Specify path to the script file$extensionHint as the first argument")
                    return COMPILATION_ERROR
                }

                val scriptArgs = arguments.freeArgs.subList(1, arguments.freeArgs.size)
                return KotlinToJVMBytecodeCompiler.compileAndExecuteScript(environment, scriptArgs)
            } else {
                if (destination != null) {
                    if (destination.endsWith(".jar")) {
                        configuration.put(JVMConfigurationKeys.OUTPUT_JAR, File(destination))
                    } else {
                        configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(destination))
                    }
                }

                val environment = createCoreEnvironment(rootDisposable, configuration, arguments, messageCollector)
                        ?: return COMPILATION_ERROR

                registerJavacIfNeeded(environment, arguments).let {
                    if (!it) return COMPILATION_ERROR
                }

                if (environment.getSourceFiles().isEmpty()) {
                    if (arguments.version) {
                        return OK
                    }
                    messageCollector.report(ERROR, "No source files")
                    return COMPILATION_ERROR
                }

                KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment)

                compileJavaFilesIfNeeded(environment, arguments).let {
                    if (!it) return COMPILATION_ERROR
                }
            }

            if (arguments.reportPerf) {
                reportGCTime(configuration)
                reportCompilationTime(configuration)
                PerformanceCounter.report { s -> reportPerf(configuration, s) }
            }
            return OK
        } catch (e: CompilationException) {
            messageCollector.report(
                EXCEPTION,
                OutputMessageUtil.renderException(e),
                MessageUtil.psiElementToMessageLocation(e.element)
            )
            return INTERNAL_ERROR
        }
    }

    override fun setupPlatformSpecificLanguageFeatureSettings(
        extraLanguageFeatures: MutableMap<LanguageFeature, LanguageFeature.State>,
        commandLineArguments: K2JVMCompilerArguments
    ) {
        if (commandLineArguments.strictJavaNullabilityAssertions) {
            extraLanguageFeatures[LanguageFeature.StrictJavaNullabilityAssertions] = LanguageFeature.State.ENABLED
        }

        if (commandLineArguments.properIeee754Comparisons) {
            extraLanguageFeatures[LanguageFeature.ProperIeee754Comparisons] = LanguageFeature.State.ENABLED
        }

        super.setupPlatformSpecificLanguageFeatureSettings(extraLanguageFeatures, commandLineArguments)
    }

    private fun registerJavacIfNeeded(
        environment: KotlinCoreEnvironment,
        arguments: K2JVMCompilerArguments
    ): Boolean {
        if (arguments.useJavac) {
            environment.configuration.put(JVMConfigurationKeys.USE_JAVAC, true)
            if (arguments.compileJava) {
                environment.configuration.put(JVMConfigurationKeys.COMPILE_JAVA, true)
            }
            return environment.registerJavac(arguments = arguments.javacArguments)
        }

        return true
    }

    private fun compileJavaFilesIfNeeded(
        environment: KotlinCoreEnvironment,
        arguments: K2JVMCompilerArguments
    ): Boolean {
        if (arguments.compileJava) {
            return JavacWrapper.getInstance(environment.project).use { it.compile() }
        }
        return true
    }

    private fun createCoreEnvironment(
        rootDisposable: Disposable,
        configuration: CompilerConfiguration,
        arguments: K2JVMCompilerArguments,
        messageCollector: MessageCollector
    ): KotlinCoreEnvironment? {
        val scriptResolverEnv = createScriptResolverEnvironment(arguments, messageCollector) ?: return null
        configureScriptDefinitions(arguments.scriptTemplates, configuration, messageCollector, scriptResolverEnv)
        if (messageCollector.hasErrors()) return null

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        if (initStartNanos != 0L) {
            val initNanos = System.nanoTime() - initStartNanos
            reportPerf(configuration, "INIT: Compiler initialized in " + TimeUnit.NANOSECONDS.toMillis(initNanos) + " ms")
            initStartNanos = 0L
        }

        if (!messageCollector.hasErrors()) {
            scriptResolverEnv.put("projectRoot", environment.project.run { basePath ?: baseDir?.canonicalPath }?.let(::File))
            return environment
        }

        return null
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: K2JVMCompilerArguments, services: Services
    ) {
        if (IncrementalCompilation.isEnabled()) {
            services.get(LookupTracker::class.java)?.let {
                configuration.put(CommonConfigurationKeys.LOOKUP_TRACKER, it)
            }

            services.get(ExpectActualTracker::class.java)?.let {
                configuration.put(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, it)
            }

            services.get(IncrementalCompilationComponents::class.java)?.let {
                configuration.put(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS, it)
            }

            services.get(JavaClassesTracker::class.java)?.let {
                configuration.put(JVMConfigurationKeys.JAVA_CLASSES_TRACKER, it)
            }
        }

        arguments.additionalJavaModules?.let { additionalJavaModules ->
            configuration.addAll(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, additionalJavaModules.toList())
        }
    }

    override fun createArguments(): K2JVMCompilerArguments = K2JVMCompilerArguments().apply {
        if (System.getenv("KOTLIN_REPORT_PERF") != null) {
            reportPerf = true
        }
    }

    override fun executableScriptFileName(): String = "kotlinc-jvm"

    companion object {
        private var initStartNanos = System.nanoTime()
        // allows to track GC time for each run when repeated compilation is used
        private val elapsedGCTime = hashMapOf<String, Long>()
        private var elapsedJITTime = 0L

        fun resetInitStartTime() {
            if (initStartNanos == 0L) {
                initStartNanos = System.nanoTime()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            CLITool.doMain(K2JVMCompiler(), args)
        }

        fun reportPerf(configuration: CompilerConfiguration, message: String) {
            if (!configuration.getBoolean(CLIConfigurationKeys.REPORT_PERF)) return

            configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(INFO, "PERF: $message")
        }

        fun reportGCTime(configuration: CompilerConfiguration) {
            ManagementFactory.getGarbageCollectorMXBeans().forEach {
                val currentTime = it.collectionTime
                val elapsedTime = elapsedGCTime.getOrElse(it.name) { 0 }
                val time = currentTime - elapsedTime
                reportPerf(configuration, "GC time for ${it.name} is $time ms")
                elapsedGCTime[it.name] = currentTime
            }
        }

        fun reportCompilationTime(configuration: CompilerConfiguration) {
            val bean = ManagementFactory.getCompilationMXBean() ?: return
            val currentTime = bean.totalCompilationTime
            reportPerf(configuration, "JIT time is ${currentTime - elapsedJITTime} ms")
            elapsedJITTime = currentTime
        }

        private fun putAdvancedOptions(configuration: CompilerConfiguration, arguments: K2JVMCompilerArguments) {
            configuration.put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, arguments.noCallAssertions)
            configuration.put(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS, arguments.noReceiverAssertions)
            configuration.put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, arguments.noParamAssertions)
            configuration.put(
                JVMConfigurationKeys.NO_EXCEPTION_ON_EXPLICIT_EQUALS_FOR_BOXED_NULL,
                arguments.noExceptionOnExplicitEqualsForBoxedNull
            )
            configuration.put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, arguments.noOptimize)

            val constructorCallNormalizationMode =
                JVMConstructorCallNormalizationMode.fromStringOrNull(arguments.constructorCallNormalizationMode)
            if (constructorCallNormalizationMode == null) {
                configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                    ERROR,
                    "Unknown constructor call normalization mode: ${arguments.constructorCallNormalizationMode}, " +
                            "supported modes: ${JVMConstructorCallNormalizationMode.values().map { it.description }}"
                )
            }
            configuration.put(
                JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE,
                constructorCallNormalizationMode ?: JVMConstructorCallNormalizationMode.DEFAULT
            )

            configuration.put(JVMConfigurationKeys.INHERIT_MULTIFILE_PARTS, arguments.inheritMultifileParts)
            configuration.put(JVMConfigurationKeys.SKIP_RUNTIME_VERSION_CHECK, arguments.skipRuntimeVersionCheck)
            configuration.put(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING, !arguments.useOldClassFilesReading)

            if (arguments.useOldClassFilesReading) {
                configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                    .report(INFO, "Using the old java class files reading implementation")
            }

            configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
            configuration.put(CLIConfigurationKeys.REPORT_PERF, arguments.reportPerf)
            configuration.put(JVMConfigurationKeys.USE_SINGLE_MODULE, arguments.singleModule)
            configuration.put(JVMConfigurationKeys.ADD_BUILT_INS_FROM_COMPILER_TO_DEPENDENCIES, arguments.addCompilerBuiltIns)
            configuration.put(JVMConfigurationKeys.CREATE_BUILT_INS_FROM_MODULE_DEPENDENCIES, arguments.loadBuiltInsFromDependencies)

            arguments.declarationsOutputPath?.let { configuration.put(JVMConfigurationKeys.DECLARATIONS_JSON_PATH, it) }
        }

        private fun configureContentRoots(paths: KotlinPaths?, arguments: K2JVMCompilerArguments, configuration: CompilerConfiguration) {
            val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            for (path in arguments.classpath?.split(File.pathSeparatorChar).orEmpty()) {
                configuration.add(JVMConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(path)))
            }

            for (modularRoot in arguments.javaModulePath?.split(File.pathSeparatorChar).orEmpty()) {
                configuration.add(JVMConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(File(modularRoot)))
            }

            val isModularJava = configuration.get(JVMConfigurationKeys.JDK_HOME).let { it != null && CoreJrtFileSystem.isModularJdk(it) }
            fun addRoot(moduleName: String, libraryName: String, getLibrary: (KotlinPaths) -> File, noLibraryArgument: String) {
                val file = getLibraryFromHome(paths, getLibrary, libraryName, messageCollector, noLibraryArgument) ?: return
                if (isModularJava) {
                    configuration.add(JVMConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    configuration.add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, moduleName)
                } else {
                    configuration.add(JVMConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(file))
                }
            }

            if (!arguments.noStdlib) {
                addRoot("kotlin.stdlib", PathUtil.KOTLIN_JAVA_STDLIB_JAR, KotlinPaths::getStdlibPath, "'-no-stdlib'")
                addRoot("kotlin.script.runtime", PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR, KotlinPaths::getScriptRuntimePath, "'-no-stdlib'")
            }
            // "-no-stdlib" implies "-no-reflect": otherwise we would be able to transitively read stdlib classes through kotlin-reflect,
            // which is likely not what user wants since s/he manually provided "-no-stdlib"
            if (!arguments.noReflect && !arguments.noStdlib) {
                addRoot("kotlin.reflect", PathUtil.KOTLIN_JAVA_REFLECT_JAR, KotlinPaths::getReflectPath, "'-no-reflect' or '-no-stdlib'")
            }
        }

        private fun configureJdkHome(
            arguments: K2JVMCompilerArguments,
            configuration: CompilerConfiguration,
            messageCollector: MessageCollector
        ): ExitCode {
            if (arguments.noJdk) {
                configuration.put(JVMConfigurationKeys.NO_JDK, true)

                if (arguments.jdkHome != null) {
                    messageCollector.report(STRONG_WARNING, "The '-jdk-home' option is ignored because '-no-jdk' is specified")
                }
                return OK
            }

            if (arguments.jdkHome != null) {
                val jdkHome = File(arguments.jdkHome)
                if (!jdkHome.exists()) {
                    messageCollector.report(ERROR, "JDK home directory does not exist: $jdkHome")
                    return COMPILATION_ERROR
                }

                messageCollector.report(LOGGING, "Using JDK home directory $jdkHome")

                configuration.put(JVMConfigurationKeys.JDK_HOME, jdkHome)
            }

            return OK
        }

        fun configureScriptDefinitions(
            scriptTemplates: Array<String>?,
            configuration: CompilerConfiguration,
            messageCollector: MessageCollector,
            scriptResolverEnv: HashMap<String, Any?>
        ) {
            val classpath = configuration.jvmClasspathRoots
            // TODO: consider using escaping to allow kotlin escaped names in class names
            if (scriptTemplates != null && scriptTemplates.isNotEmpty()) {
                val classloader =
                    URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), Thread.currentThread().contextClassLoader)
                var hasErrors = false
                for (template in scriptTemplates) {
                    try {
                        val cls = classloader.loadClass(template)
                        val def = KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, scriptResolverEnv)
                        configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, def)
                        messageCollector.report(
                            INFO,
                            "Added script definition $template to configuration: files pattern = \"${def.scriptFilePattern}\", " +
                                    "resolver = ${def.dependencyResolver.javaClass.name}"
                        )
                    } catch (ex: ClassNotFoundException) {
                        messageCollector.report(ERROR, "Cannot find script definition template class $template")
                        hasErrors = true
                    } catch (ex: Exception) {
                        messageCollector.report(ERROR, "Error processing script definition template $template: ${ex.message}")
                        hasErrors = true
                        break
                    }
                }
                if (hasErrors) {
                    messageCollector.report(LOGGING, "(Classpath used for templates loading: $classpath)")
                    return
                }
            }
            configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, StandardScriptDefinition)
        }

        fun createScriptResolverEnvironment(arguments: K2JVMCompilerArguments, messageCollector: MessageCollector): HashMap<String, Any?>? {
            val scriptResolverEnv = hashMapOf<String, Any?>()
            // parses key/value pairs in the form <key>=<value>, where
            //   <key> - is a single word (\w+ pattern)
            //   <value> - optionally quoted string with allowed escaped chars (only double-quote and backslash chars are supported)
            // TODO: implement generic unescaping
            val envParseRe = """(\w+)=(?:"([^"\\]*(\\.[^"\\]*)*)"|([^\s]*))""".toRegex()
            val unescapeRe = """\\(["\\])""".toRegex()
            if (arguments.scriptResolverEnvironment != null) {
                for (envParam in arguments.scriptResolverEnvironment!!) {
                    val match = envParseRe.matchEntire(envParam)
                    if (match == null || match.groupValues.size < 4 || match.groupValues[1].isBlank()) {
                        messageCollector.report(ERROR, "Unable to parse script-resolver-environment argument $envParam")
                        return null
                    }
                    scriptResolverEnv.put(
                        match.groupValues[1],
                        match.groupValues.drop(2).firstOrNull { it.isNotEmpty() }?.let { unescapeRe.replace(it, "\$1") })
                }
            }
            return scriptResolverEnv
        }
    }
}

fun main(args: Array<String>) = K2JVMCompiler.main(args)
