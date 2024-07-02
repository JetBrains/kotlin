/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import com.intellij.psi.PsiJavaModule.MODULE_INFO_FILE
import com.intellij.util.lang.JavaVersion
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.addModularRootIfNotNull
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.MockLibraryUtil.compileJavaFilesLibraryToJar
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.handlers.PhasedIrDumpHandler
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ASSERTIONS_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.ENABLE_DEBUG_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JVM_TARGET
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.LAMBDAS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.PROVIDE_JAVA_AS_BINARIES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.SAM_CONVERSIONS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.SERIALIZE_IR
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.STRING_CONCAT
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.DISABLE_CALL_ASSERTIONS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.DISABLE_PARAM_ASSERTIONS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.EMIT_JVM_TYPE_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ENABLE_JVM_IR_INLINER
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ENABLE_JVM_PREVIEW
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.JDK_RELEASE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LINK_VIA_SIGNATURES_K1
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.NO_NEW_JAVA_ANNOTATION_TARGETS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.NO_UNIFIED_NULL_CHECKS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OLD_INNER_CLASSES_LOGIC
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.PARAMETERS_METADATA
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.USE_INLINE_SCOPES_NUMBERS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.USE_TYPE_TABLE
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
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

open class JvmEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        val TEST_CONFIGURATION_KIND_KEY = CompilerConfigurationKey.create<ConfigurationKind>("ConfigurationKind")

        private val DEFAULT_JVM_TARGET_FROM_PROPERTY: String? = System.getProperty("kotlin.test.default.jvm.target")
        const val DEFAULT_JVM_VERSION_PROPERTY: String = "kotlin.test.default.jvm.version"
        val DEFAULT_JVM_VERSION_FROM_PROPERTY: String? = System.getProperty(DEFAULT_JVM_VERSION_PROPERTY)

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

        fun getLibraryFilesExceptRealRuntime(
            testServices: TestServices,
            configurationKind: ConfigurationKind,
            directives: RegisteredDirectives
        ): List<File> {
            val provider = testServices.standardLibrariesPathProvider
            val files = mutableListOf<File>()
            if (configurationKind.withRuntime) {
                files.add(provider.kotlinTestJarForTests())
            } else if (configurationKind.withMockRuntime) {
                files.add(provider.minimalRuntimeJarForTests())
                files.add(provider.scriptRuntimeJarForTests())
            }
            if (configurationKind.withReflection) {
                files.add(provider.reflectJarForTests())
            }
            files.add(provider.getAnnotationsJar())

            if (JvmEnvironmentConfigurationDirectives.STDLIB_JDK8 in directives) {
                files.add(provider.runtimeJarForTestsWithJdk8())
            }
            return files
        }

        fun getJdkHome(jdkKindTestJdkKind: TestJdkKind): File? = when (jdkKindTestJdkKind) {
            TestJdkKind.MOCK_JDK -> null
            TestJdkKind.MODIFIED_MOCK_JDK -> null
            TestJdkKind.FULL_JDK_11 -> KtTestUtil.getJdk11Home()
            TestJdkKind.FULL_JDK_17 -> KtTestUtil.getJdk17Home()
            TestJdkKind.FULL_JDK_21 -> KtTestUtil.getJdk21Home()
            TestJdkKind.FULL_JDK -> getJdkHomeFromProperty {
                runIf(JavaVersion.current() >= JavaVersion.compose(9)) { File(System.getProperty("java.home")) }
            }
            TestJdkKind.ANDROID_API -> null
        }

        inline fun getJdkHomeFromProperty(onNull: () -> File?): File? {
            return when (val version = DEFAULT_JVM_VERSION_FROM_PROPERTY) {
                "1.8" -> KtTestUtil.getJdk8Home()
                "11" -> KtTestUtil.getJdk11Home()
                "17" -> KtTestUtil.getJdk17Home()
                "21" -> KtTestUtil.getJdk21Home()
                null -> onNull()
                else -> error("Unknown JDK version: \"$DEFAULT_JVM_VERSION_PROPERTY=$version\". Only following versions are allowed: [1.8, 11, 17, 21]")
            }
        }

        fun getJdkClasspathRoot(jdkKind: TestJdkKind): File? = when (jdkKind) {
            TestJdkKind.MOCK_JDK -> KtTestUtil.findMockJdkRtJar()
            TestJdkKind.MODIFIED_MOCK_JDK -> KtTestUtil.findMockJdkRtModified()
            TestJdkKind.ANDROID_API -> KtTestUtil.findAndroidApiJar()
            TestJdkKind.FULL_JDK_11 -> null
            TestJdkKind.FULL_JDK_17 -> null
            TestJdkKind.FULL_JDK_21 -> null
            TestJdkKind.FULL_JDK -> null
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
        register(DISABLE_PARAM_ASSERTIONS, JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS)
        register(DISABLE_CALL_ASSERTIONS, JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS)
        register(NO_UNIFIED_NULL_CHECKS, JVMConfigurationKeys.NO_UNIFIED_NULL_CHECKS)
        register(PARAMETERS_METADATA, JVMConfigurationKeys.PARAMETERS_METADATA)
        register(JVM_TARGET, JVMConfigurationKeys.JVM_TARGET)
        register(SERIALIZE_IR, JVMConfigurationKeys.SERIALIZE_IR)
        register(JDK_RELEASE, JVMConfigurationKeys.JDK_RELEASE)
        register(USE_TYPE_TABLE, JVMConfigurationKeys.USE_TYPE_TABLE)
        register(ENABLE_DEBUG_MODE, JVMConfigurationKeys.ENABLE_DEBUG_MODE)
        register(NO_NEW_JAVA_ANNOTATION_TARGETS, JVMConfigurationKeys.NO_NEW_JAVA_ANNOTATION_TARGETS)
        register(OLD_INNER_CLASSES_LOGIC, JVMConfigurationKeys.OLD_INNER_CLASSES_LOGIC)
        register(LINK_VIA_SIGNATURES_K1, JVMConfigurationKeys.LINK_VIA_SIGNATURES)
        register(ENABLE_JVM_IR_INLINER, JVMConfigurationKeys.ENABLE_IR_INLINER)
        register(USE_INLINE_SCOPES_NUMBERS, JVMConfigurationKeys.USE_INLINE_SCOPES_NUMBERS)
        register(USE_PSI_CLASS_FILES_READING, JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING)
        register(ALLOW_KOTLIN_PACKAGE, CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE)
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.targetPlatform !in JvmPlatforms.allJvmPlatforms) return
        configureDefaultJvmTarget(configuration)
        val registeredDirectives = module.directives

        val jdkKind = extractJdkKind(registeredDirectives)
        getJdkHome(jdkKind)?.let { configuration.put(JVMConfigurationKeys.JDK_HOME, it) }
        getJdkClasspathRoot(jdkKind)?.let { configuration.addJvmClasspathRoot(it) }

        when (jdkKind) {
            TestJdkKind.MOCK_JDK, TestJdkKind.MODIFIED_MOCK_JDK, TestJdkKind.ANDROID_API -> {
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
            }

            TestJdkKind.FULL_JDK_11 -> {}
            TestJdkKind.FULL_JDK_17 -> {}
            TestJdkKind.FULL_JDK_21 -> {}
            TestJdkKind.FULL_JDK -> {}
        }

        val configurationKind = extractConfigurationKind(registeredDirectives).also {
            configuration.put(TEST_CONFIGURATION_KIND_KEY, it)
        }

        if (configurationKind.withRuntime) {
            configuration.configureStandardLibs(
                testServices.standardLibrariesPathProvider,
                K2JVMCompilerArguments().also { it.noReflect = true }
            )
        }
        configuration.addJvmClasspathRoots(getLibraryFilesExceptRealRuntime(testServices, configurationKind, module.directives))

        val isIr = module.targetBackend?.isIR != false
        configuration.put(JVMConfigurationKeys.IR, isIr)
        configuration.putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create())

        if (CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES in module.directives) {
            configuration.putCustomPhaseConfigWithEnabledDump(module)
        }

        configuration.put(JVMConfigurationKeys.VALIDATE_BYTECODE, true)
        configuration.configureJdkClasspathRoots()

        configuration.registerModuleDependencies(module)

        configuration.addJavaBinaryRootsByCompiledJavaModulesFromModuleDependencies(configurationKind, module)

        val javaFiles = module.javaFiles.ifEmpty { return }
        javaFiles.forEach { testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it) }
        val javaModuleInfoFiles = javaFiles.filter { it.name == MODULE_INFO_FILE }

        if (PROVIDE_JAVA_AS_BINARIES !in registeredDirectives) {
            if (javaModuleInfoFiles.isNotEmpty()) {
                configuration.addJavaSourceRootsByJavaModules(javaModuleInfoFiles)
            } else {
                configuration.addJavaSourceRoot(testServices.sourceFileProvider.getJavaSourceDirectoryForModule(module))
            }

            // we add this as a part of the classpath only when Java files are being analyzed as sources,
            // so that relevant annotations & classes are resolved properly
            configuration.addJvmClasspathRoots(
                testServices.additionalClassPathForJavaCompilationOrAnalysis?.classPath?.map(::File).orEmpty()
            )
        } else {
            if (javaModuleInfoFiles.isNotEmpty()) {
                configuration.addJavaBinaryRootsByJavaModules(configurationKind, javaModuleInfoFiles)
            } else {
                val jvmClasspathRoots = configuration.jvmClasspathRoots.map { it.absolutePath }
                val additionalClassPath = testServices.additionalClassPathForJavaCompilationOrAnalysis?.classPath.orEmpty()
                configuration.addJvmClasspathRoot(
                    compileJavaFilesLibraryToJar(
                        testServices.sourceFileProvider.getJavaSourceDirectoryForModule(module).path,
                        "${module.name}-$JAVA_BINARIES_JAR_NAME",
                        extraClasspath = jvmClasspathRoots + additionalClassPath,
                        assertions = JUnit5Assertions,
                        useJava11 = registeredDirectives[JDK_KIND].singleOrNull() == TestJdkKind.FULL_JDK_11,
                    )
                )
            }
        }
    }

    private fun CompilerConfiguration.addJavaSourceRootsByJavaModules(
        javaModuleInfoFiles: List<TestFile>
    ) {
        for (javaModuleInfoFile in javaModuleInfoFiles) {
            val javaModuleDir = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(javaModuleInfoFile).parentFile
            addJavaSourceRoot(javaModuleDir)
        }
    }

    private fun CompilerConfiguration.addJavaBinaryRootsByJavaModules(
        configurationKind: ConfigurationKind,
        javaModuleInfoFiles: List<TestFile>
    ) {
        val classPath = jvmClasspathRoots.map(File::getAbsolutePath)
        val modulePath = buildList {
            addAll(jvmModularRoots.map(File::getAbsolutePath))
            if (configurationKind.withRuntime) {
                add(testServices.standardLibrariesPathProvider.runtimeJarForTests().path)
            }
        }
        for (javaModuleInfoFile in javaModuleInfoFiles) {
            val javaModuleDir = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(javaModuleInfoFile).parentFile
            val javaModuleName = javaModuleInfoFile.relativePath.substringBefore('/')
            val javaModuleBinary = compileJavaFilesLibraryToJar(
                javaModuleDir.path,
                "$javaModuleName-$JAVA_BINARIES_JAR_NAME",
                extraClasspath = classPath,
                extraModulepath = modulePath,
                assertions = JUnit5Assertions,
                useJava11 = true
            )
            addModularRootIfNotNull(isModularJava = true, javaModuleName, javaModuleBinary)
        }
    }

    private fun CompilerConfiguration.addJavaBinaryRootsByCompiledJavaModulesFromModuleDependencies(
        configurationKind: ConfigurationKind,
        module: TestModule
    ) {
        val moduleDependencies = module.allDependencies.map { testServices.dependencyProvider.getTestModule(it.moduleName) }
        val javaModuleInfoFilesFromModuleDependencies = moduleDependencies.mapNotNull { moduleDependency ->
            moduleDependency.javaFiles.singleOrNull { javaFile -> javaFile.name == MODULE_INFO_FILE }
        }
        addJavaBinaryRootsByJavaModules(configurationKind, javaModuleInfoFilesFromModuleDependencies)
    }

    private fun configureDefaultJvmTarget(configuration: CompilerConfiguration) {
        if (DEFAULT_JVM_TARGET_FROM_PROPERTY == null) return
        val customDefaultTarget = JvmTarget.fromString(DEFAULT_JVM_TARGET_FROM_PROPERTY)
            ?: error("Can't construct JvmTarget for $DEFAULT_JVM_TARGET_FROM_PROPERTY")
        val originalTarget = configuration[JVMConfigurationKeys.JVM_TARGET]
        if (originalTarget == null || customDefaultTarget.majorVersion > originalTarget.majorVersion) {
            // It's not safe to substitute target in general
            // because it can affect generated bytecode and original behaviour should be tested somehow.
            // Original behaviour testing is performed by
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
                toDumpStateBefore = phases,
                toDumpStateAfter = phases,
                dumpToDirectory = dumpDirectory.absolutePath
            )
            put(CLIConfigurationKeys.PHASE_CONFIG, phaseConfig)
        }
    }

    private fun CompilerConfiguration.registerModuleDependencies(module: TestModule) {
        addJvmClasspathRoots(module.allDependencies.filter { it.kind == DependencyKind.Binary }.toFileList())

        val isJava9Module = module.files.any(TestFile::isModuleInfoJavaFile)
        for (dependency in module.allDependencies.filter { it.kind == DependencyKind.Binary }.toFileList()) {
            if (isJava9Module) {
                add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(dependency))
            }
            addJvmClasspathRoot(dependency)
        }

        val binaryFriends = module.friendDependencies.filter { it.kind == DependencyKind.Binary }
        if (binaryFriends.isNotEmpty()) {
            put(JVMConfigurationKeys.FRIEND_PATHS, binaryFriends.toFileList().map { it.absolutePath })
        }
    }

    private fun List<DependencyDescription>.toFileList(): List<File> = this.flatMap(::convertDependencyToFileList)

    protected open fun convertDependencyToFileList(dependency: DependencyDescription): List<File> {
        val friendModule = testServices.dependencyProvider.getTestModule(dependency.moduleName)
        return listOf(testServices.compiledClassesManager.compileKotlinToDiskAndGetOutputDir(friendModule, classFileFactory = null))
    }
}

// This should be used as a classpath entries when Java classes are being compiled/analyzed to become a binary/source dependencies of kt-files
// But in case the Java files are compiled it should not belong to the classpath of the module where test kt-files belong.
// Currently, it's used for jsr305.jar, for which it's necessary to make sure that once some library uses the annotation from that jar,
// we still don't need the jar itself when using the library to read the annotations.
// (as they have been written to the class-files as fully-qualified names)
class AdditionalClassPathForJavaCompilationOrAnalysis(val classPath: List<String>) : TestService

val TestServices.additionalClassPathForJavaCompilationOrAnalysis by TestServices.nullableTestServiceAccessor<AdditionalClassPathForJavaCompilationOrAnalysis>()
