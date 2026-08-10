/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiJavaModule
import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_WARNING
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.diagnosticFactoriesStorage
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil
import org.jetbrains.kotlin.cli.jvm.compiler.applyModuleProperties
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportException
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.JavaRootPath
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File

object JvmConfigurationPipelinePhase : AbstractConfigurationPhase<K2JVMCompilerArguments>(
    name = "JvmConfigurationPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
    configurationUpdaters = listOf(JvmConfigurationUpdater)
) {
    override fun executePhase(input: ArgumentsPipelineArtifact<K2JVMCompilerArguments>): ConfigurationPipelineArtifact =
        super.executePhase(input).also {
            val configuration = it.configuration
            val dumpModelDir = configuration[CommonConfigurationKeys.DUMP_MODEL]
            if (dumpModelDir != null) {
                JvmFrontendPipelinePhase.dumpModel(dumpModelDir, configuration.moduleChunk!!.modules, configuration, input.arguments)
            }
        }

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }

    override fun provideCustomScriptingPluginOptions(arguments: K2JVMCompilerArguments): List<String> {
        return buildList {
            if (arguments.scriptTemplates.isNotEmpty()) {
                add("plugin:kotlin.scripting:script-templates=${arguments.scriptTemplates.joinToString(",")}")
            }
            if (arguments.scriptResolverEnvironment.isNotEmpty()) {
                add("plugin:kotlin.scripting:script-resolver-environment=${arguments.scriptResolverEnvironment.joinToString(",")}")
            }
        }
    }
}

object JvmConfigurationUpdater : ConfigurationUpdater<K2JVMCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JVMCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        configuration.diagnosticFactoriesStorage?.registerDiagnosticContainers(JvmBackendErrors)

        (val arguments, val services, val _ = rootDisposable, val _ = messageCollector, val _ = performanceManager) = input
        configuration.reportLog("Configuring the compilation environment")

        arguments.buildFile?.let { configuration.buildFile = File(it) }
        configuration.allowNoSourceFiles = arguments.allowNoSourceFiles
        configuration.setupJvmSpecificArguments(arguments)
        configuration.setupIncrementalCompilationServices(arguments, services)

        configuration.phaseConfig = createPhaseConfig(arguments, jvmPhases).also {
            if (arguments.listPhases) it.list(jvmPhases)
        }
        if (!configuration.configureJdkHome(arguments)) return
        configuration.disableStandardScriptDefinition = arguments.disableStandardScript
        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.moduleName = moduleName
        configuration.configureJavaModulesContentRoots(arguments)
        configuration.configureStandardLibs(configuration.kotlinPaths, arguments)
        configuration.configureAdvancedJvmOptions(arguments)
        if (arguments.expression == null) {
            configuration.setupModuleChunk(arguments)
        } else {
            configuration.configureContentRootsFromClassPath(arguments)
        }

        configuration.put(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION, arguments.disableStandardScript)

        if (arguments.script || arguments.expression != null) {
            configuration.scriptMode = arguments.script
            configuration.freeArgsForScript += arguments.freeArgs
            configuration.expressionToEvaluate = arguments.expression
            configuration.defaultExtensionForScripts = arguments.defaultScriptExtension
        } else {
            configuration.replMode = @Suppress("DEPRECATION") arguments.repl
            configuration.freeArgsForScript += arguments.freeArgs
        }
        // should be called after configuring jdk home from build file
        configuration.configureJdkClasspathRoots()
        configuration.targetPlatform = JvmPlatforms.defaultJvmPlatform
    }

    private fun CompilerConfiguration.setupIncrementalCompilationServices(arguments: K2JVMCompilerArguments, services: Services) {
        // used by Build Tools API in non-incremental compilations:
        lookupTracker = services[LookupTracker::class.java]
        importTracker = services[ImportTracker::class.java]

        if (!incrementalCompilationIsEnabled(arguments)) return
        expectActualTracker = services[ExpectActualTracker::class.java]
        inlineConstTracker = services[InlineConstTracker::class.java]
        enumWhenTracker = services[EnumWhenTracker::class.java]
        fileMappingTracker = services[ICFileMappingTracker::class.java]
        incrementalCompilationComponents = services[IncrementalCompilationComponents::class.java]

        if (arguments.multiPlatform && arguments.useMetadataOnIncrementalClasspath) {
            icMetadataTracker = services[ICJvmMetadataTracker::class.java]
        }
    }

    private fun CompilerConfiguration.setupModuleChunk(arguments: K2JVMCompilerArguments) {
        val buildFile = this.buildFile
        val moduleChunk = configureModuleChunk(arguments, buildFile)
        this.moduleChunk = moduleChunk
        if (moduleChunk.modules.size == 1) {
            applyModuleProperties(moduleChunk.modules.single(), buildFile)
        }
        configureSourceRoots(moduleChunk.modules, buildFile)
    }

    private fun CompilerConfiguration.configureModuleChunk(
        arguments: K2JVMCompilerArguments,
        buildFile: File?,
    ): ModuleChunk {
        val destination = arguments.destination?.let { File(it) }

        return if (buildFile != null) {
            fun strongWarning(message: String) {
                this.report(COMPILER_ARGUMENTS_WARNING, message)
            }

            if (destination != null) {
                strongWarning("The '-d' option with a directory destination is ignored because '-Xbuild-file' is specified")
            }
            if (arguments.javaSourceRoots.isNotEmpty()) {
                strongWarning("The '-Xjava-source-roots' option is ignored because '-Xbuild-file' is specified")
            }
            if (arguments.javaPackagePrefix != null) {
                strongWarning("The '-Xjava-package-prefix' option is ignored because '-Xbuild-file' is specified")
            }
            configureContentRootsFromClassPath(arguments)
            put(JVMConfigurationKeys.MODULE_XML_FILE, buildFile)
            CompileEnvironmentUtil.loadModuleChunk(
                buildFile,
                { report(CliDiagnostics.JVM_CLI_ERROR, it) },
                { reportException(it) }
            )
        } else {
            if (destination != null) {
                if (destination.path.endsWith(".jar")) {
                    put(JVMConfigurationKeys.OUTPUT_JAR, destination)
                } else {
                    put(JVMConfigurationKeys.OUTPUT_DIRECTORY, destination)
                }
            }

            val module = ModuleBuilder(
                this[CommonConfigurationKeys.MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
                destination?.path ?: ".", "java-production"
            )
            module.configureFromArgs(arguments)

            ModuleChunk(listOf(module))
        }
    }

    private fun ModuleBuilder.configureFromArgs(args: K2JVMCompilerArguments) {
        args.friendPaths.forEach { addFriendDir(it) }
        args.classpath?.split(File.pathSeparator)?.forEach { addClasspathEntry(it) }
        args.javaSourceRoots.forEach {
            addJavaSourceRoot(JavaRootPath(it, args.javaPackagePrefix))
        }

        val commonSources = args.commonSources.toSet()
        // With `-script` flag, the first free arg is considered as a path to the script file and others are as script arguments
        if (args.script) return
        for (arg in args.freeArgs) {
            if (arg.endsWith(JavaFileType.DOT_DEFAULT_EXTENSION)) {
                addJavaSourceRoot(JavaRootPath(arg, args.javaPackagePrefix))
            } else {
                addSourceFiles(arg)
                if (arg in commonSources) {
                    addCommonSourceFiles(arg)
                }

                if (File(arg).isDirectory) {
                    addJavaSourceRoot(JavaRootPath(arg, args.javaPackagePrefix))
                }
            }
        }
    }

    fun CompilerConfiguration.configureSourceRoots(chunk: List<Module>, buildFile: File? = null) {
        val hmppCliModuleStructure = get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        for (module in chunk) {
            val commonSources = getBuildFilePaths(buildFile, module.getCommonSourceFiles()).toSet()

            for (path in getBuildFilePaths(buildFile, module.getSourceFiles())) {
                addKotlinSourceRoot(
                    path,
                    isCommon = hmppCliModuleStructure?.isFromCommonModule(path) ?: (path in commonSources),
                    hmppCliModuleStructure?.getModuleNameForSource(path)
                )
            }
        }

        for (module in chunk) {
            for ((path, packagePrefix) in module.getJavaSourceRoots()) {
                addJavaSourceRoot(File(path), packagePrefix)
            }
        }

        val isJava9Module = chunk.any { module ->
            module.getJavaSourceRoots().any { (path, packagePrefix) ->
                val file = File(path)
                packagePrefix == null &&
                        (file.name == PsiJavaModule.MODULE_INFO_FILE ||
                                (file.isDirectory && file.listFiles()!!.any { it.name == PsiJavaModule.MODULE_INFO_FILE }))
            }
        }

        for (module in chunk) {
            for (classpathRoot in module.getClasspathRoots()) {
                if (isJava9Module) {
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(File(classpathRoot)))
                }
                add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(classpathRoot)))
            }
        }

        for (module in chunk) {
            val modularJdkRoot = module.modularJdkRoot
            if (modularJdkRoot != null) {
                // We use the SDK of the first module in the chunk, which is not always correct because some other module in the chunk
                // might depend on a different SDK
                put(JVMConfigurationKeys.JDK_HOME, File(modularJdkRoot))
                break
            }
        }

        addAll(JVMConfigurationKeys.MODULES, chunk)
    }

    fun getBuildFilePaths(buildFile: File?, sourceFilePaths: List<String>): List<String> {
        return if (buildFile == null) sourceFilePaths
        else sourceFilePaths.map { path ->
            (File(path).takeIf(File::isAbsolute) ?: buildFile.resolveSibling(path)).absolutePath
        }
    }

}
