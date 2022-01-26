/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import com.intellij.openapi.util.SystemInfo
import com.intellij.psi.PsiJavaModule.MODULE_INFO_FILE
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.addModularRootIfNotNull
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.configureStandardLibs
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.MockLibraryUtil.compileJavaFilesLibraryToJar
import org.jetbrains.kotlin.test.TestJavacVersion
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.handlers.PhasedIrDumpHandler
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ALL_JAVA_AS_BINARY
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ASSERTIONS_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.COMPILE_JAVA_USING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.INCLUDE_JAVA_AS_BINARY
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JVM_TARGET
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.LAMBDAS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.SAM_CONVERSIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.SERIALIZE_IR
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.STRING_CONCAT
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.DISABLE_CALL_ASSERTIONS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.DISABLE_PARAM_ASSERTIONS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EMIT_JVM_TYPE_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ENABLE_JVM_PREVIEW
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.NO_OPTIMIZED_CALLABLE_REFERENCES
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.NO_UNIFIED_NULL_CHECKS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.PARAMETERS_METADATA
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.JDK_RELEASE
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.jvm.CompiledClassesManager
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.io.path.ExperimentalPathApi

class JvmEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        val TEST_CONFIGURATION_KIND_KEY = CompilerConfigurationKey.create<ConfigurationKind>("ConfigurationKind")

        private val DEFAULT_JVM_TARGET_FROM_PROPERTY: String? = System.getProperty("kotlin.test.default.jvm.target")

        private const val JAVA_BINARIES_JAR_NAME = "java-binaries"

        fun extractConfigurationKind(registeredDirectives: RegisteredDirectives): ConfigurationKind {
            val withStdlib = ConfigurationDirectives.WITH_STDLIB in registeredDirectives
            val withReflect = JvmEnvironmentConfigurationDirectives.WITH_REFLECT in registeredDirectives
            val noRuntime = JvmEnvironmentConfigurationDirectives.NO_RUNTIME in registeredDirectives
            if (noRuntime && withStdlib) {
                error("NO_RUNTIME and WITH_STDLIB can not be used together")
            }
            return when {
                withStdlib && !withReflect -> ConfigurationKind.NO_KOTLIN_REFLECT
                withStdlib || withReflect -> ConfigurationKind.ALL
                noRuntime -> ConfigurationKind.JDK_NO_RUNTIME
                else -> ConfigurationKind.JDK_ONLY
            }
        }

        fun extractJdkKind(registeredDirectives: RegisteredDirectives): TestJdkKind {
            val fullJdkEnabled = JvmEnvironmentConfigurationDirectives.FULL_JDK in registeredDirectives
            val jdkKinds = registeredDirectives[JvmEnvironmentConfigurationDirectives.JDK_KIND]

            if (fullJdkEnabled) {
                if (jdkKinds.isNotEmpty()) {
                    error("FULL_JDK and JDK_KIND can not be used together")
                }
                return TestJdkKind.FULL_JDK
            }

            return when (jdkKinds.size) {
                0 -> TestJdkKind.MOCK_JDK
                1 -> jdkKinds.single()
                else -> error("Too many jdk kinds passed: ${jdkKinds.joinToArrayString()}")
            }
        }

        fun getLibraryFilesExceptRealRuntime(configurationKind: ConfigurationKind, directives: RegisteredDirectives): List<File> {
            val files = mutableListOf<File>()
            if (configurationKind.withRuntime) {
                files.add(ForTestCompileRuntime.kotlinTestJarForTests())
            } else if (configurationKind.withMockRuntime) {
                files.add(ForTestCompileRuntime.minimalRuntimeJarForTests())
                files.add(ForTestCompileRuntime.scriptRuntimeJarForTests())
            }
            if (configurationKind.withReflection) {
                files.add(ForTestCompileRuntime.reflectJarForTests())
            }
            files.add(KtTestUtil.getAnnotationsJar())

            if (JvmEnvironmentConfigurationDirectives.STDLIB_JDK8 in directives) {
                files.add(ForTestCompileRuntime.runtimeJarForTestsWithJdk8())
            }
            files.add(KtTestUtil.getAnnotationsJar())
            return files
        }
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JvmEnvironmentConfigurationDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::CompiledClassesManager))

    override fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {
        register(STRING_CONCAT, JVMConfigurationKeys.STRING_CONCAT)
        register(ASSERTIONS_MODE, JVMConfigurationKeys.ASSERTIONS_MODE)
        register(SAM_CONVERSIONS, JVMConfigurationKeys.SAM_CONVERSIONS)
        register(LAMBDAS, JVMConfigurationKeys.LAMBDAS)
        register(USE_OLD_INLINE_CLASSES_MANGLING_SCHEME, JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME)
        register(ENABLE_JVM_PREVIEW, JVMConfigurationKeys.ENABLE_JVM_PREVIEW)
        register(EMIT_JVM_TYPE_ANNOTATIONS, JVMConfigurationKeys.EMIT_JVM_TYPE_ANNOTATIONS)
        register(NO_OPTIMIZED_CALLABLE_REFERENCES, JVMConfigurationKeys.NO_OPTIMIZED_CALLABLE_REFERENCES)
        register(DISABLE_PARAM_ASSERTIONS, JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS)
        register(DISABLE_CALL_ASSERTIONS, JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS)
        register(NO_UNIFIED_NULL_CHECKS, JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS)
        register(PARAMETERS_METADATA, JVMConfigurationKeys.PARAMETERS_METADATA)
        register(JVM_TARGET, JVMConfigurationKeys.JVM_TARGET)
        register(SERIALIZE_IR, JVMConfigurationKeys.SERIALIZE_IR)
        register(JDK_RELEASE, JVMConfigurationKeys.JDK_RELEASE)
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalStdlibApi::class)
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.targetPlatform !in JvmPlatforms.allJvmPlatforms) return
        configureDefaultJvmTarget(configuration)
        val registeredDirectives = module.directives

        when (extractJdkKind(registeredDirectives)) {
            TestJdkKind.MOCK_JDK -> {
                configuration.addJvmClasspathRoot(KtTestUtil.findMockJdkRtJar())
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
            }
            TestJdkKind.MODIFIED_MOCK_JDK -> {
                configuration.addJvmClasspathRoot(KtTestUtil.findMockJdkRtModified())
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
            }
            TestJdkKind.FULL_JDK_6 -> {
                val jdk6 = System.getenv("JDK_16") ?: error("Environment variable JDK_16 is not set")
                configuration.put(JVMConfigurationKeys.JDK_HOME, File(jdk6))
            }
            TestJdkKind.FULL_JDK_11 -> {
                configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk11Home())
            }
            TestJdkKind.FULL_JDK_17 -> {
                configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk17Home())
            }
            TestJdkKind.FULL_JDK -> {
                if (SystemInfo.IS_AT_LEAST_JAVA9) {
                    configuration.put(JVMConfigurationKeys.JDK_HOME, File(System.getProperty("java.home")))
                }
            }
            TestJdkKind.ANDROID_API -> {
                configuration.addJvmClasspathRoot(KtTestUtil.findAndroidApiJar())
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
            }
        }

        val configurationKind = extractConfigurationKind(registeredDirectives).also {
            configuration.put(TEST_CONFIGURATION_KIND_KEY, it)
        }

        val javaVersionToCompile = registeredDirectives[COMPILE_JAVA_USING].singleOrNull()
        val javaBinaryFiles = if (ALL_JAVA_AS_BINARY !in registeredDirectives) {
            module.javaFiles.filter { INCLUDE_JAVA_AS_BINARY in it.directives }
        } else module.javaFiles

        val useJava11ToCompileIncludedJavaFiles = javaVersionToCompile == TestJavacVersion.JAVAC_11

        if (configurationKind.withRuntime) {
            configuration.configureStandardLibs(PathUtil.kotlinPathsForDistDirectory, K2JVMCompilerArguments().also { it.noReflect = true })
        }
        configuration.addJvmClasspathRoots(getLibraryFilesExceptRealRuntime(configurationKind, module.directives))

        val isIr = module.targetBackend?.isIR == true
        configuration.put(JVMConfigurationKeys.IR, isIr)

        val javaSourceFiles = module.javaFiles.filter { INCLUDE_JAVA_AS_BINARY !in it.directives }

        if (javaSourceFiles.isNotEmpty() && JvmEnvironmentConfigurationDirectives.SKIP_JAVA_SOURCES !in module.directives && ALL_JAVA_AS_BINARY !in registeredDirectives) {
            javaSourceFiles.forEach { testServices.sourceFileProvider.getRealFileForSourceFile(it) }

            // TODO: temporary hack to provide java 9 modules in the source mode properly (see comment on ClasspathRootsResolved::addModularRoots)
            addJavaCompiledModulesFromDependentKotlinModules(configuration, configurationKind, module, bySources = true)

            val moduleInfoFiles = javaSourceFiles.filter { it.name == MODULE_INFO_FILE }

            if (moduleInfoFiles.isNotEmpty()) {
                addJavaSourceRootsByJavaModules(configuration, moduleInfoFiles)
            } else {
                configuration.addJavaSourceRoot(testServices.sourceFileProvider.javaSourceDirectory)
            }
        }

        if (javaBinaryFiles.isNotEmpty()) {
            javaBinaryFiles.forEach { testServices.sourceFileProvider.getRealFileForBinaryFile(it) }

            addJavaCompiledModulesFromDependentKotlinModules(configuration, configurationKind, module, bySources = false)

            val moduleInfoFiles = javaBinaryFiles.filter { it.name == MODULE_INFO_FILE }

            // TODO: Use module graph to build proper modulepath for each module according cross-module dependencies
            if (moduleInfoFiles.isNotEmpty()) {
                addJavaBinaryRootsByJavaModules(configuration, configurationKind, moduleInfoFiles)
            } else {
                configuration.addJvmClasspathRoot(
                    compileJavaFilesLibraryToJar(
                        testServices.sourceFileProvider.javaBinaryDirectory.path,
                        JAVA_BINARIES_JAR_NAME,
                        extraClasspath = configuration.jvmClasspathRoots.map { it.absolutePath },
                        assertions = JUnit5Assertions,
                        useJava11 = useJava11ToCompileIncludedJavaFiles
                    )
                )
            }
        }

        configuration.registerModuleDependencies(module)

        if (JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING in module.directives) {
            configuration.put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, true)
        }

        if (LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives) {
            configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, true)
        }

        if (CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES in module.directives) {
            configuration.putCustomPhaseConfigWithEnabledDump(module)
        }

        configuration.put(JVMConfigurationKeys.VALIDATE_IR, true)
        configuration.put(JVMConfigurationKeys.VALIDATE_BYTECODE, true)
        configuration.configureJdkClasspathRoots()
    }

    private fun addJavaSourceRootsByJavaModules(configuration: CompilerConfiguration, moduleInfoFiles: List<TestFile>) {
        val javaSourceDirectory = testServices.sourceFileProvider.javaSourceDirectory
        for (moduleInfoFile in moduleInfoFiles) {
            val moduleName = moduleInfoFile.relativePath.substringBefore('/')
            val moduleDir = File("${javaSourceDirectory.path}/$moduleName").also { it.mkdir() }
            configuration.addJavaSourceRoot(moduleDir)
        }
    }

    private fun addJavaBinaryRootsByJavaModules(
        configuration: CompilerConfiguration,
        configurationKind: ConfigurationKind,
        moduleInfoFiles: List<TestFile>
    ) {
        val javaBinaryDirectory = testServices.sourceFileProvider.javaBinaryDirectory
        for (moduleInfoFile in moduleInfoFiles) {
            val moduleName = moduleInfoFile.relativePath.substringBefore('/')
            addJavaCompiledModule(configuration, configurationKind, moduleName, bySources = true, targetDir = javaBinaryDirectory)
        }
    }

    private fun addJavaCompiledModulesFromDependentKotlinModules(
        configuration: CompilerConfiguration,
        configurationKind: ConfigurationKind,
        module: TestModule,
        bySources: Boolean
    ) {
        val moduleDependencies = module.allDependencies.map { testServices.dependencyProvider.getTestModule(it.moduleName) }
        val filterJavaModuleInfoFiles = { testFile: TestFile ->
            val binaryFilesFilter = INCLUDE_JAVA_AS_BINARY in testFile.directives || ALL_JAVA_AS_BINARY in module.directives
            val includeOrExcludeBinaryFilesFilter = (bySources && !binaryFilesFilter) || (!bySources && binaryFilesFilter)
            includeOrExcludeBinaryFilesFilter && testFile.name == MODULE_INFO_FILE
        }
        val moduleInfoFilesFromDependencies = moduleDependencies.mapNotNull { it.javaFiles.singleOrNull(filterJavaModuleInfoFiles) }

        for (dependentModuleInfoFile in moduleInfoFilesFromDependencies) {
            val moduleName = dependentModuleInfoFile.relativePath.substringBefore('/')
            addJavaCompiledModule(configuration, configurationKind, moduleName, bySources)
        }
    }

    private fun addJavaCompiledModule(
        configuration: CompilerConfiguration,
        configurationKind: ConfigurationKind,
        moduleName: String,
        bySources: Boolean,
        targetDir: File = testServices.sourceFileProvider.run { if (bySources) javaSourceDirectory else javaBinaryDirectory }
    ) {
        val moduleDir = File("${targetDir.path}/$moduleName")
        val javaBinaries = if (bySources) {
            compileJavaFilesToModularJar(configuration, configurationKind, moduleDir)
        } else {
            File("${moduleDir.path}/$JAVA_BINARIES_JAR_NAME.jar")
        }

        configuration.addModularRootIfNotNull(isModularJava = true, moduleName, javaBinaries)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun compileJavaFilesToModularJar(
        configuration: CompilerConfiguration,
        configurationKind: ConfigurationKind,
        sourcesDir: File
    ): File {
        val modulePath = buildList {
            addAll(configuration.jvmModularRoots.map { it.absolutePath })
            if (configurationKind.withRuntime) {
                add(ForTestCompileRuntime.runtimeJarForTests().path)
            }
        }
        return MockLibraryUtil.compileLibraryToJar(
            sourcesDir.path,
            sourcesDir,
            JAVA_BINARIES_JAR_NAME,
            extraClasspath = configuration.jvmClasspathRoots.map { it.absolutePath },
            extraModulepath = modulePath,
            assertions = JUnit5Assertions,
            useJava11 = true
        )
    }

    private fun configureDefaultJvmTarget(configuration: CompilerConfiguration) {
        if (DEFAULT_JVM_TARGET_FROM_PROPERTY == null) return
        val customDefaultTarget = JvmTarget.fromString(DEFAULT_JVM_TARGET_FROM_PROPERTY)
            ?: error("Can't construct JvmTarget for $DEFAULT_JVM_TARGET_FROM_PROPERTY")
        val originalTarget = configuration[JVMConfigurationKeys.JVM_TARGET]
        if (originalTarget == null || customDefaultTarget.majorVersion > originalTarget.majorVersion) {
            // It's not safe to substitute target in general
            // cause it can affect generated bytecode and original behaviour should be tested somehow.
            // Original behaviour testing is perfomed by
            //
            //      codegenTest(target = 6, jvm = "Last", jdk = mostRecentJdk)
            //      codegenTest(target = 8, jvm = "Last", jdk = mostRecentJdk)
            //
            // in compiler/tests-different-jdk/build.gradle.kts
            configuration.put(JVMConfigurationKeys.JVM_TARGET, customDefaultTarget)
        }
    }

    private fun CompilerConfiguration.putCustomPhaseConfigWithEnabledDump(module: TestModule) {
        val dumpDirectory = testServices.getOrCreateTempDirectory(PhasedIrDumpHandler.DUMPED_IR_FOLDER_NAME)
        val phases = module.directives[CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES].toSet()
        if (phases.isNotEmpty()) {
            val phaseConfig = PhaseConfig(
                jvmPhases,
                toDumpStateAfter = phases,
                dumpToDirectory = dumpDirectory.absolutePath
            )
            put(CLIConfigurationKeys.PHASE_CONFIG, phaseConfig)
        }
    }

    private fun CompilerConfiguration.registerModuleDependencies(module: TestModule) {
        addJvmClasspathRoots(module.allDependencies.filter { it.kind == DependencyKind.Binary }.toFileList())

        val binaryFriends = module.friendDependencies.filter { it.kind == DependencyKind.Binary }
        if (binaryFriends.isNotEmpty()) {
            put(JVMConfigurationKeys.FRIEND_PATHS, binaryFriends.toFileList().map { it.absolutePath })
        }
    }

    private fun List<DependencyDescription>.toFileList(): List<File> = this.flatMap { dependency ->
        val friendModule = testServices.dependencyProvider.getTestModule(dependency.moduleName)
        listOfNotNull(
            testServices.compiledClassesManager.getCompiledKotlinDirForModule(friendModule),
            testServices.compiledClassesManager.getCompiledJavaDirForModule(friendModule)
        )
    }
}
