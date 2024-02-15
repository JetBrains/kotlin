/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.InTextDirectivesUtils.IGNORE_BACKEND_DIRECTIVE_PREFIXES
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.utils.TransformersFunctions.Android
import java.io.File
import java.io.IOException

class CodegenTestsOnAndroidGenerator private constructor(private val pathManager: PathManager) {
    private var currentModuleIndex = 1

    private val pathFilter: String? = System.getProperties().getProperty("kotlin.test.android.path.filter")

    private val pendingTestSourceFileGenerators = hashMapOf<String, TestSourceFileGenerator>()

    //keep it globally to avoid test grouping on TC
    private val generatedTestNames = hashSetOf<String>()

    private val common = FlavorConfig(TargetBackend.ANDROID, "common", 4)
    private val reflect = FlavorConfig(TargetBackend.ANDROID, "reflect")
    private val commonIr = FlavorConfig(TargetBackend.ANDROID_IR, "common_ir", 4)
    private val reflectIr = FlavorConfig(TargetBackend.ANDROID_IR, "reflect_ir")

    class FlavorConfig(private val backend: TargetBackend, private val prefix: String, private val limit: Int = 1) {
        private var writtenFilesCount = 0

        fun printStatistics() {
            println("FlavorTestCompiler for $backend: $prefix, generated file count: $writtenFilesCount")
        }

        fun getFlavorNameForNewFiles(newFilesCount: Int): String {
            writtenFilesCount += newFilesCount
            // Allocating up to 2500 files per folder should be fine for each app flavor,
            // thus avoiding the need for multidex, which is necessary when there are more than 64K methods.
            // Each folder will be archived using build.gradle.kts.
            val index = writtenFilesCount / 2500
            val name = "$prefix$index"
            check(index < limit) { "Please add a new flavor in build.gradle for $name" }
            return name
        }
    }

    private fun generateTestsAndFlavourSuites() {
        println("Clearing destination folder")
        pathManager.prepareDestinationFolder()
        println("Generating test files")

        generateTestMethodsForDirectories(
            TargetBackend.ANDROID,
            common,
            reflect,
            pathManager.testDataDirectories
        )

        generateTestMethodsForDirectories(
            TargetBackend.ANDROID_IR,
            commonIr,
            reflectIr,
            pathManager.testDataDirectories
        )

        pendingTestSourceFileGenerators.values.forEach { it.generate() }
    }

    private fun generateTestMethodsForDirectories(
        backend: TargetBackend,
        commonFlavor: FlavorConfig,
        reflectionFlavor: FlavorConfig,
        dirs: List<File>,
    ) {
        val holders = mutableMapOf<ConfigurationKey, FilesWriter>()

        for (dir in dirs) {
            val files = dir.listFiles() ?: throw IOException("Cannot list files in $dir")
            processFiles(files, holders, backend, commonFlavor, reflectionFlavor)
        }

        holders.values.forEach {
            it.writeFilesOnDisk()
        }

        commonFlavor.printStatistics()
        reflectionFlavor.printStatistics()
    }

    internal inner class FilesWriter(
        private val flavorConfig: FlavorConfig,
        private val configuration: CompilerConfiguration,
    ) {
        private val rawFiles = arrayListOf<TestClassInfo>()
        private val testInfos = arrayListOf<TestInfo>()

        private fun shouldWriteFilesOnDisk(): Boolean = rawFiles.size > 300

        fun writeFilesOnDiskIfNeeded() {
            if (shouldWriteFilesOnDisk()) {
                writeFilesOnDisk()
            }
        }

        fun writeFilesOnDisk() {
            val disposable = Disposer.newDisposable("Disposable for ${FilesWriter::class.qualifiedName}.writeFilesOnDisk")
            val environment = KotlinCoreEnvironment.createForTests(
                disposable,
                configuration.copy().apply {
                    put(CommonConfigurationKeys.MODULE_NAME, "android-module-" + currentModuleIndex++)
                },
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            try {
                compileAndWriteFiles(
                    rawFiles.map {
                        try {
                            CodegenTestFiles.create(it.name, it.content, environment.project).psiFile
                        } catch (e: Throwable) {
                            throw RuntimeException("Error on processing ${it.name}:\n${it.content}", e)
                        }
                    }, environment, testInfos
                )
            } finally {
                rawFiles.clear()
                testInfos.clear()
                Disposer.dispose(disposable)
            }
        }

        private fun compileAndWriteFiles(
            files: List<KtFile>,
            environment: KotlinCoreEnvironment,
            testInfos: List<TestInfo>
        ) {
            if (files.isEmpty()) return

            val flavorName = flavorConfig.getFlavorNameForNewFiles(files.size)

            val outputDir = pathManager.prepareFlavorTestClassesDirectory(flavorName)
            println("Generating ${files.size} files into ${outputDir.name}, configuration: '${environment.configuration}'...")

            val outputFiles = GenerationUtils.compileFiles(files, environment).run {
                destroy()
                factory
            }

            val testSourceFileGenerator = pendingTestSourceFileGenerators.getOrPut(flavorName) {
                TestSourceFileGenerator(
                    pathManager.prepareFlavorTestSourceDirectory(flavorName),
                    flavorName,
                    generatedTestNames
                )
            }
            testSourceFileGenerator.addTests(testInfos)
            outputFiles.writeAllTo(outputDir)
        }

        fun addTest(testFiles: Collection<TestClassInfo>, info: TestInfo) {
            rawFiles.addAll(testFiles)
            testInfos.add(info)
        }
    }

    private fun processFiles(
        files: Array<File>,
        holders: MutableMap<ConfigurationKey, FilesWriter>,
        backend: TargetBackend,
        commmonFlavor: FlavorConfig,
        reflectionFlavor: FlavorConfig,
    ) {
        holders.values.forEach {
            it.writeFilesOnDiskIfNeeded()
        }

        for (file in files) {
            if (file.isDirectory) {
                processFiles(
                    file.listFiles() ?: throw IOException("Cannot list files in $file"),
                    holders,
                    backend,
                    commmonFlavor,
                    reflectionFlavor
                )
            } else if (FileUtilRt.getExtension(file.name) != KotlinFileType.EXTENSION) {
                // skip non kotlin files
            } else {
                if (pathFilter != null && !file.path.contains(pathFilter)) {
                    continue
                }

                if (!InTextDirectivesUtils.isPassingTarget(backend.compatibleWith, file) ||
                    InTextDirectivesUtils.isIgnoredTarget(
                        TargetBackend.ANDROID, file, /*includeAny=*/ true, *IGNORE_BACKEND_DIRECTIVE_PREFIXES
                    )
                ) {
                    continue
                }

                val fullFileText = FileUtil.loadFile(file, true)

                if (fullFileText.contains("// WITH_COROUTINES")) {
                    if (fullFileText.contains("kotlin.coroutines.experimental")) continue
                    if (fullFileText.contains("// LANGUAGE_VERSION: 1.2")) continue
                }

                //TODO support JvmPackageName
                if (fullFileText.contains("@file:JvmPackageName(")) continue

                // TODO: Support jvm assertions
                if (fullFileText.contains("// ASSERTIONS_MODE: jvm")) continue

                if (fullFileText.contains("// MODULE: ")) continue

                // TODO: add && backend != TargetBackend.JVM_IR
                if (fullFileText.contains("// IGNORE_BACKEND_K1")) continue

                if (fullFileText.contains("// IGNORE_DEXING")) continue
                val targets = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fullFileText, "// JVM_TARGET:")

                val isAtLeastJvm8Target = !targets.contains(JvmTarget.JVM_1_6.description)

                // TODO: type annotations are supported on Android SDK 26. A separate flavor can be created
                if (isAtLeastJvm8Target && fullFileText.contains("@Target(AnnotationTarget.TYPE)")) continue

                // TODO: JDK 1.8 features are supported on Android SDK 26
                if (fullFileText.contains("// SKIP_JDK6")) continue

                if (!fullFileText.contains("fun box()")) continue

                val testConfiguration = createTestConfiguration(file, backend)
                val services = testConfiguration.testServices

                val moduleStructure = try {
                    testConfiguration.moduleStructureExtractor.splitTestDataByModules(
                        file.path,
                        testConfiguration.directives,
                    ).also {
                        services.register(TestModuleStructure::class, it)
                    }
                } catch (e: ExceptionFromModuleStructureTransformer) {
                    continue
                }
                val module = moduleStructure.modules.singleOrNull() ?: continue
                if (module.files.any { it.isJavaFile || it.isKtsFile }) continue
                if (module.files.isEmpty()) continue
                services.registerDependencyProvider(DependencyProviderImpl(services, moduleStructure.modules))

                val keyConfiguration = CompilerConfiguration()
                val configuratorForFlags = JvmEnvironmentConfigurator(services)
                with(configuratorForFlags) {
                    val extractor = DirectiveToConfigurationKeyExtractor()
                    extractor.provideConfigurationKeys()
                    extractor.configure(keyConfiguration, module.directives)
                }
                val kind = JvmEnvironmentConfigurator.extractConfigurationKind(module.directives)
                val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(module.directives)

                keyConfiguration.languageVersionSettings = module.languageVersionSettings

                val key = ConfigurationKey(kind, jdkKind, keyConfiguration.toString())
                val compiler = if (kind.withReflection) reflectionFlavor else commmonFlavor
                val compilerConfigurationProvider = services.compilerConfigurationProvider as CompilerConfigurationProviderImpl
                val filesHolder = holders.getOrPut(key) {
                    println("Creating new configuration by $key")
                    FilesWriter(compiler, compilerConfigurationProvider.createCompilerConfiguration(module))
                }

                patchFilesAndAddTest(file, module, services, filesHolder)
            }
        }
    }

    private fun createTestConfiguration(testDataFile: File, backend: TargetBackend): TestConfiguration {
        return TestConfigurationBuilder().apply {
            configure(backend)
            testInfo = KotlinTestInfo(
                "org.jetbrains.kotlin.android.tests.AndroidRunner",
                "test${testDataFile.nameWithoutExtension.replaceFirstChar(Char::uppercaseChar)}",
                emptySet()
            )
            startingArtifactFactory = { ResultingArtifact.Source() }
        }.build(testDataFile.path)
    }

    private fun TestConfigurationBuilder.configure(backend: TargetBackend) {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetBackend = backend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::CodegenHelpersSourceFilesProvider,
        )

        assertions = JUnit5Assertions
        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        useAdditionalService<ApplicationDisposableProvider> { ExecutionListenerBasedDisposableProvider() }
        useAdditionalService<KotlinStandardLibrariesPathProvider> { StandardLibrariesPathProviderForKotlinProject }
        useSourcePreprocessor(*AbstractKotlinCompilerTest.defaultPreprocessors.toTypedArray())
        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())
        class AndroidTransformingPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
            override fun process(file: TestFile, content: String): String {
                val transformers = Android.forAll + (Android.forSpecificFile[file.originalFile]?.let { listOf(it) } ?: emptyList())
                return transformers.fold(content) { text, transformer -> transformer(text) }
            }
        }
        useSourcePreprocessor({ AndroidTransformingPreprocessor(it) })
    }

    companion object {
        const val TEST_CLASS_PACKAGE = "org.jetbrains.kotlin.android.tests"
        const val BASE_TEST_CLASS_PACKAGE = "org.jetbrains.kotlin.android.tests"
        const val BASE_TEST_CLASS_NAME = "AbstractCodegenTestCaseOnAndroid"

        /**
         * Generates Android test sources and classes.
         *
         * @param args The first argument is destination directory, the rest are test data directories to walk.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val destinationDirectory = File(requireNotNull(args.getOrNull(0)) { "No destination directory provided" })
            val testDataDirectories = args.drop(1).map(::File)
            require(testDataDirectories.isNotEmpty()) { "No test data directories provided" }
            val notTestData = testDataDirectories.filterNot { "testData" in it.path }
            require(notTestData.isEmpty()) { "Directories unrelated to testData are provided: $notTestData" }
            println("Generating Android te`st sources and classes to $destinationDirectory")
            val pathManager = PathManager(destinationDirectory, testDataDirectories)
            CodegenTestsOnAndroidGenerator(pathManager).generateTestsAndFlavourSuites()
            println("Generated Android test sources and classes to $destinationDirectory")
        }
    }
}
