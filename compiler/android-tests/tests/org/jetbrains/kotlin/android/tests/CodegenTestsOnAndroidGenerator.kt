/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
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
import org.jetbrains.kotlin.test.utils.TransformersFunctions.Android
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assert
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.assertTrue

data class ConfigurationKey(val kind: ConfigurationKind, val jdkKind: TestJdkKind, val configuration: String)

class CodegenTestsOnAndroidGenerator private constructor(private val pathManager: PathManager) {
    private var currentModuleIndex = 1

    private val pathFilter: String? = System.getProperties().getProperty("kotlin.test.android.path.filter")

    private val pendingUnitTestGenerators = hashMapOf<String, UnitTestFileWriter>()

    //keep it globally to avoid test grouping on TC
    private val generatedTestNames = hashSetOf<String>()

    private val COMMON = FlavorConfig(TargetBackend.ANDROID,"common", 4)
    private val REFLECT = FlavorConfig(TargetBackend.ANDROID, "reflect", 1)

    private val COMMON_IR = FlavorConfig(TargetBackend.ANDROID_IR, "common_ir", 4)
    private val REFLECT_IR = FlavorConfig(TargetBackend.ANDROID_IR,"reflect_ir", 1)

    class FlavorConfig(private val backend: TargetBackend, private val prefix: String, val limit: Int) {

        private var writtenFilesCount = 0

        fun printStatistics() {
            println("FlavorTestCompiler for $backend: $prefix, generated file count: $writtenFilesCount")
        }

        fun getFlavorForNewFiles(newFilesCount: Int): String {
            writtenFilesCount += newFilesCount
            //2500 files per folder that would be used by flavor to avoid multidex usage,
            // each folder would be jared by build.gradle script
            val index = writtenFilesCount / 2500

            return getFlavorName(index, prefix).also {
                assertTrue("Please Add  new flavor in build.gradle for $it") { index < limit }
            }
        }

        private fun getFlavorName(index: Int, prefix: String): String {
            return prefix + index
        }

    }

    private fun prepareAndroidModuleAndGenerateTests(skipSdkDirWriting: Boolean) {
        prepareAndroidModule(skipSdkDirWriting)
        generateTestsAndFlavourSuites()
    }

    private fun prepareAndroidModule(skipSdkDirWriting: Boolean) {
        FileUtil.copyDir(File(pathManager.androidModuleRoot), File(pathManager.tmpFolder))
        if (!skipSdkDirWriting) {
            writeAndroidSkdToLocalProperties(pathManager)
        }

        println("Copying kotlin-stdlib.jar and kotlin-reflect.jar in android module...")
        copyKotlinRuntimeJars()
        copyGradleWrapperAndPatch()
    }

    private fun copyGradleWrapperAndPatch() {
        val projectRoot = File(pathManager.tmpFolder)
        val target = File(projectRoot, "gradle/wrapper")
        File("./gradle/wrapper/").copyRecursively(target)
        val gradlew = File(projectRoot, "gradlew")
        File("./gradlew").copyTo(gradlew).also {
            if (!SystemInfo.isWindows) {
                it.setExecutable(true)
            }
        }
        File("./gradlew.bat").copyTo(File(projectRoot, "gradlew.bat"))
        val file = File(target, "gradle-wrapper.properties")
        file.readLines().map {
            when {
                it.startsWith("distributionUrl") -> "distributionUrl=https\\://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
                it.startsWith("distributionSha256Sum") -> "distributionSha256Sum=$GRADLE_SHA_256"
                else -> it
            }
        }.let { lines ->
            FileWriter(file).use { fw ->
                lines.forEach { line ->
                    fw.write("$line\n")
                }
            }
        }
    }


    private fun copyKotlinRuntimeJars() {
        FileUtil.copy(
            ForTestCompileRuntime.runtimeJarForTests(),
            File(pathManager.libsFolderInAndroidTmpFolder + "/kotlin-stdlib.jar")
        )
        FileUtil.copy(
            ForTestCompileRuntime.reflectJarForTests(),
            File(pathManager.libsFolderInAndroidTmpFolder + "/kotlin-reflect.jar")
        )

        FileUtil.copy(
            ForTestCompileRuntime.kotlinTestJarForTests(),
            File(pathManager.libsFolderInAndroidTmpFolder + "/kotlin-test.jar")
        )
    }

    private fun generateTestsAndFlavourSuites() {
        println("Generating test files...")

        val folders = arrayOf(
            File("compiler/testData/codegen/box"),
            File("compiler/testData/codegen/boxInline")
        )

        generateTestMethodsForDirectories(
            TargetBackend.ANDROID,
            COMMON,
            REFLECT,
            *folders
        )

        generateTestMethodsForDirectories(
            TargetBackend.ANDROID_IR,
            COMMON_IR,
            REFLECT_IR,
            *folders
        )

        pendingUnitTestGenerators.values.forEach { it.generate() }
    }

    private fun generateTestMethodsForDirectories(
        backend: TargetBackend,
        commonFlavor: FlavorConfig,
        reflectionFlavor: FlavorConfig,
        vararg dirs: File
    ) {
        val holders = mutableMapOf<ConfigurationKey, FilesWriter>()

        for (dir in dirs) {
            val files = dir.listFiles() ?: error("Folder with testData is empty: ${dir.absolutePath}")
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
        private val configuration: CompilerConfiguration
    ) {
        private val rawFiles = arrayListOf<TestClassInfo>()
        private val unitTestDescriptions = arrayListOf<TestInfo>()

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
                writeFiles(
                    rawFiles.map {
                        try {
                            CodegenTestFiles.create(it.name, it.content, environment.project).psiFile
                        } catch (e: Throwable) {
                            throw RuntimeException("Error on processing ${it.name}:\n${it.content}", e)
                        }
                    }, environment, unitTestDescriptions
                )
            } finally {
                rawFiles.clear()
                unitTestDescriptions.clear()
                Disposer.dispose(disposable)
            }
        }

        private fun writeFiles(
            filesToCompile: List<KtFile>,
            environment: KotlinCoreEnvironment,
            unitTestDescriptions: ArrayList<TestInfo>
        ) {
            if (filesToCompile.isEmpty()) return

            val flavorName = flavorConfig.getFlavorForNewFiles(filesToCompile.size)

            val outputDir = File(pathManager.getOutputForCompiledFiles(flavorName))
            println("Generating ${filesToCompile.size} files into ${outputDir.name}, configuration: '${environment.configuration}'...")

            val outputFiles = GenerationUtils.compileFiles(filesToCompile, environment).run { destroy(); factory }

            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            Assert.assertTrue("Cannot create directory for compiled files", outputDir.exists())
            val unitTestFileWriter = pendingUnitTestGenerators.getOrPut(flavorName) {
                UnitTestFileWriter(
                    getFlavorUnitTestFolder(flavorName),
                    flavorName,
                    generatedTestNames
                )
            }
            unitTestFileWriter.addTests(unitTestDescriptions)
            outputFiles.writeAllTo(outputDir)
        }

        private fun getFlavorUnitTestFolder(flavourName: String): String {
            return pathManager.srcFolderInAndroidTmpFolder +
                    "/androidTest${flavourName.replaceFirstChar(Char::uppercaseChar)}/java/" +
                    testClassPackage.replace(".", "/") + "/"
        }

        fun addTest(testFiles: List<TestClassInfo>, info: TestInfo) {
            rawFiles.addAll(testFiles)
            unitTestDescriptions.add(info)
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    @Throws(IOException::class)
    private fun processFiles(
        files: Array<File>,
        holders: MutableMap<ConfigurationKey, FilesWriter>,
        backend: TargetBackend,
        commmonFlavor: FlavorConfig,
        reflectionFlavor: FlavorConfig
    ) {
        holders.values.forEach {
            it.writeFilesOnDiskIfNeeded()
        }

        for (file in files) {
            if (file.isDirectory) {
                val listFiles = file.listFiles()
                if (listFiles != null) {
                    processFiles(listFiles, holders, backend, commmonFlavor, reflectionFlavor)
                }
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
                if (fullFileText.contains("// IGNORE_BACKEND_K1")) continue
                val targets = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fullFileText, "// JVM_TARGET:")

                val isAtLeastJvm8Target = !targets.contains(JvmTarget.JVM_1_6.description)

                if (isAtLeastJvm8Target && fullFileText.contains("@Target(AnnotationTarget.TYPE)")) {
                    //TODO: type annotations supported on sdk 26 emulator
                    continue
                }

                // TODO: support SKIP_JDK6 on new platforms
                if (fullFileText.contains("// SKIP_JDK6")) continue

                if (hasBoxMethod(fullFileText)) {
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
                        FilesWriter(compiler, compilerConfigurationProvider.createCompilerConfiguration(module)).also {
                            println("Creating new configuration by $key")
                        }
                    }

                    patchFilesAndAddTest(file, module, services, filesHolder)
                }
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
        const val GRADLE_VERSION = "6.8.1" // update GRADLE_SHA_256 on change
        const val GRADLE_SHA_256 = "fd591a34af7385730970399f473afabdb8b28d57fd97d6625c388d090039d6fd"
        const val testClassPackage = "org.jetbrains.kotlin.android.tests"
        const val testClassName = "CodegenTestCaseOnAndroid"
        const val baseTestClassPackage = "org.jetbrains.kotlin.android.tests"
        const val baseTestClassName = "AbstractCodegenTestCaseOnAndroid"


        @JvmOverloads
        @JvmStatic
        @Throws(Throwable::class)
        fun generate(pathManager: PathManager, skipSdkDirWriting: Boolean = false) {
            CodegenTestsOnAndroidGenerator(pathManager).prepareAndroidModuleAndGenerateTests(skipSdkDirWriting)
        }

        private fun hasBoxMethod(text: String): Boolean {
            return text.contains("fun box()")
        }

        @Throws(IOException::class)
        internal fun writeAndroidSkdToLocalProperties(pathManager: PathManager) {
            val sdkRoot = KtTestUtil.getAndroidSdkSystemIndependentPath()
            println("Writing android sdk to local.properties: $sdkRoot")
            val file = File(pathManager.tmpFolder + "/local.properties")
            FileWriter(file).use { fw -> fw.write("sdk.dir=$sdkRoot") }
        }

        @OptIn(ExperimentalPathApi::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val tmpFolder = createTempDirectory().toAbsolutePath().toString()
            println("Created temporary folder for android tests: $tmpFolder")
            val rootFolder = Path("").toAbsolutePath().toString()
            val pathManager = PathManager(rootFolder, tmpFolder)
            generate(pathManager, true)
            println("Android test project is generated into $tmpFolder folder")
        }
    }
}
