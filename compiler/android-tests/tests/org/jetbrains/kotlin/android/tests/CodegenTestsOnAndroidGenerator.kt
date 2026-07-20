/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.additionalIrCheckers
import org.jetbrains.kotlin.config.disableIrCheckers
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.InTextDirectivesUtils.IGNORE_BACKEND_DIRECTIVE_PREFIXES
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.isApplicableTo
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicUnstableAndK2LanguageFeaturesSkipConfigurator
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.preprocessors.JvmInlineSourceTransformer
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.utils.TransformersFunctions.Android
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.assertTrue

data class ConfigurationKey(val kind: ConfigurationKind, val jdkKind: TestJdkKind, val configuration: String)

private fun CompilerConfiguration.copyWithOwnContentRoots(): CompilerConfiguration =
    copy().also { copy ->
        copy.get(CLIConfigurationKeys.CONTENT_ROOTS)?.let { contentRoots ->
            copy.put(CLIConfigurationKeys.CONTENT_ROOTS, contentRoots.toMutableList())
        }
    }

internal class AndroidTestPlan(
    val tests: List<AndroidPlannedTest>,
    private val generator: CodegenTestsOnAndroidGenerator
) {
    val flavorsToRun: List<String> = tests.map { it.flavorName }.distinct()

    fun compile(test: AndroidPlannedTest) {
        generator.compile(test)
    }

    fun generateUnitTestFiles() {
        generator.generateUnitTestFiles()
    }
}

internal data class AndroidPlannedTest(
    val testFiles: List<TestClassInfo>,
    val info: TestInfo,
    val flavorName: String,
    val configuration: CompilerConfiguration,
    val moduleName: String,
) {
    val displayName: String get() = UnitTestFileWriter.testNameForFileName(info.file.name)

    companion object {
        val ROOT_PATH: File by lazy { Path("../testData").toAbsolutePath().toFile() }
    }
}

class CodegenTestsOnAndroidGenerator private constructor(private val pathManager: PathManager) {
    private var currentModuleIndex = 1

    private val pathFilter: String? = System.getProperty("kotlin.test.android.path.filter")

    private val plannedTests = arrayListOf<AndroidPlannedTest>()
    private val compiledTestNames: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val flavorOutputLocks = ConcurrentHashMap<String, Any>()

    //keep it globally to avoid test grouping on TC
    private val generatedTestNames = hashSetOf<String>()

    private val commonFlavor = FlavorConfig("common", 5)
    private val reflectFlavor = FlavorConfig("reflect", 1)

    class FlavorConfig(private val prefix: String, val limit: Int) {

        private var writtenFilesCount = 0

        fun printStatistics() {
            println("FlavorTestCompiler for ${TargetBackend.ANDROID}: $prefix, generated file count: $writtenFilesCount")
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
        val plan = prepareAndroidModuleAndDiscoverTests(skipSdkDirWriting)
        plan.tests.forEach(plan::compile)
        plan.generateUnitTestFiles()
    }

    private fun prepareAndroidModuleAndDiscoverTests(skipSdkDirWriting: Boolean): AndroidTestPlan {
        prepareAndroidModule(skipSdkDirWriting)
        return discoverTests()
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
        val gradleWrapper = File(System.getProperty("kotlin.test.android.gradleWrapper"))
        val gradlew = File(System.getProperty("kotlin.test.android.gradlew"))
        val gradlewBat = File(System.getProperty("kotlin.test.android.gradlewBat"))

        val projectRoot = File(pathManager.tmpFolder)
        val target = File(projectRoot, "gradle/wrapper")
        gradleWrapper.copyRecursively(target)
        gradlew.copyTo(File(projectRoot, "gradlew")).also {
            if (!SystemInfo.isWindows) {
                it.setExecutable(true)
            }
        }
        gradlewBat.copyTo(File(projectRoot, "gradlew.bat"))
        val file = File(target, "gradle-wrapper.properties")
        file.readLines().map {
            when {
                it.startsWith("distributionUrl") -> "distributionUrl=https\\://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
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

    private fun discoverTests(): AndroidTestPlan {
        println("Discovering Android test files...")

        val folders = arrayOf(
            ForTestCompileRuntime.transformTestDataPath("compiler/testData/codegen/box"),
            ForTestCompileRuntime.transformTestDataPath("compiler/testData/codegen/boxJvm"),
            ForTestCompileRuntime.transformTestDataPath("compiler/testData/codegen/boxInline")
        )

        generateTestMethodsForDirectories(commonFlavor, reflectFlavor, *folders)

        return AndroidTestPlan(plannedTests.toList(), this)
    }

    private fun generateTestMethodsForDirectories(
        commonFlavor: FlavorConfig,
        reflectionFlavor: FlavorConfig,
        vararg dirs: File
    ) {
        val holders = mutableMapOf<ConfigurationKey, FilesWriter>()

        for (dir in dirs) {
            val files = dir.listFiles() ?: error("Folder with testData is empty: ${dir.absolutePath}")
            processFiles(files, holders, commonFlavor, reflectionFlavor)
        }

        commonFlavor.printStatistics()
        reflectionFlavor.printStatistics()
    }

    internal inner class FilesWriter(
        private val flavorConfig: FlavorConfig,
        private val configuration: CompilerConfiguration
    ) {
        fun addTest(testFiles: List<TestClassInfo>, info: TestInfo) {
            plannedTests += AndroidPlannedTest(
                testFiles = testFiles,
                info = TestInfo(UnitTestFileWriter.generateTestName(info.file.name, generatedTestNames), info.fqName, info.file),
                flavorName = flavorConfig.getFlavorForNewFiles(testFiles.size),
                configuration = configuration.copyWithOwnContentRoots(),
                moduleName = "android-module-" + currentModuleIndex++,
            )
        }

    }

    internal fun compile(test: AndroidPlannedTest) {
        val disposable = Disposer.newDisposable("Disposable for ${CodegenTestsOnAndroidGenerator::class.qualifiedName}.compile")

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.createForParallelTests(
            disposable,
            test.configuration.apply {
                put(CommonConfigurationKeys.MODULE_NAME, test.moduleName)
                // KT-84021 Use full K/JVM stdlib, not minimal K/JVM stdlib
                addJvmClasspathRoot(ForTestCompileRuntime.runtimeJarForTests())
                addJvmClasspathRoot(ForTestCompileRuntime.kotlinTestJarForTests())
            },
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        try {
            writeFiles(
                test.testFiles.map {
                    try {
                        CodegenTestFiles.create(it.name, it.content, environment.project).psiFile
                    } catch (e: Throwable) {
                        throw RuntimeException("Error on processing ${it.name}:\n${it.content}", e)
                    }
                }, environment, test
            )
            compiledTestNames += test.info.name
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }

    private fun writeFiles(
        filesToCompile: List<KtFile>,
        environment: KotlinCoreEnvironment,
        test: AndroidPlannedTest
    ) {
        if (filesToCompile.isEmpty()) return

        val outputDir = File(pathManager.getOutputForCompiledFiles(test.flavorName))
        println("Generating ${filesToCompile.size} from ${test.info.name} (${test.info.fqName}) files into ${outputDir.name}...")

        val state = GenerationUtils.compileFiles(filesToCompile, environment)

        synchronized(flavorOutputLocks.computeIfAbsent(test.flavorName) { Any() }) {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            assertTrue(outputDir.exists(), "Cannot create directory for compiled files")
            state.factory.writeAllTo(outputDir)
        }
    }

    private fun getFlavorUnitTestFolder(flavorName: String): String {
        return pathManager.srcFolderInAndroidTmpFolder +
                "/androidTest${flavorName.replaceFirstChar(Char::uppercaseChar)}/java/" +
                testClassPackage.replace(".", "/") + "/"
    }

    internal fun generateUnitTestFiles() {
        val missingTests = plannedTests.map { it.info.name }.filterNot(compiledTestNames::contains)
        assertTrue(missingTests.isEmpty(), "Compilation phase did not compile tests: ${missingTests.joinToString()}")
        for (entry in plannedTests.groupBy { it.flavorName }) {
            val flavorName = entry.key
            UnitTestFileWriter(
                getFlavorUnitTestFolder(flavorName),
                flavorName,
            ).also { writer ->
                writer.addTests(entry.value.map { it.info })
                writer.generate()
            }
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    @Throws(IOException::class)
    private fun processFiles(
        files: Array<File>,
        holders: MutableMap<ConfigurationKey, FilesWriter>,
        commonFlavor: FlavorConfig,
        reflectionFlavor: FlavorConfig
    ) {
        for (file in files) {
            if (file.isDirectory) {
                val listFiles = file.listFiles()
                if (listFiles != null) {
                    processFiles(listFiles, holders, commonFlavor, reflectionFlavor)
                }
            } else if (FileUtilRt.getExtension(file.name) != KotlinFileType.EXTENSION) {
                // skip non kotlin files
            } else {
                if (pathFilter != null && !file.path.contains(pathFilter)) {
                    continue
                }

                if (!InTextDirectivesUtils.isPassingTarget(TargetBackend.JVM_IR, file) ||
                    InTextDirectivesUtils.isIgnoredTarget(
                        TargetBackend.ANDROID, file, /*includeAny=*/ true, *IGNORE_BACKEND_DIRECTIVE_PREFIXES
                    )
                ) {
                    continue
                }

                val fullFileText = FileUtil.loadFile(file, true)

                // Cannot dex -> cannot run
                if (fullFileText.contains("// IGNORE_DEXING")) continue

                //TODO support JvmPackageName
                if (fullFileText.contains("@file:JvmPackageName(")) continue
                // TODO: Support jvm assertions
                if (fullFileText.contains("// ASSERTIONS_MODE: jvm")) continue
                if (fullFileText.contains("// MODULE: ")) continue
                val targets = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fullFileText, "// JVM_TARGET:")

                val isAtLeastJvm8Target = !targets.contains(JvmTarget.JVM_1_6.description)

                if (isAtLeastJvm8Target && fullFileText.contains("@Target(AnnotationTarget.TYPE)")) {
                    //TODO: type annotations supported on sdk 26 emulator
                    continue
                }

                // TODO: support SKIP_JDK6 on new platforms
                if (fullFileText.contains("// SKIP_JDK6")) continue

                if (hasBoxMethod(fullFileText)) {
                    val testConfiguration = createTestConfiguration(file)
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
                    services.registerArtifactsProvider(ArtifactsProvider())

                    // The configuration is used as a key here and not used for the actual compiler invocation
                    // So if the configuration is created with default services inside, it messes up the
                    // equals/hashcode.
                    @OptIn(CompilerConfiguration.Internals::class)
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
                    keyConfiguration.disableIrCheckers = IrCheckersDisabledByTestDirectives.filter { it.key.isApplicableTo(module, services) }.values.toList()
                    keyConfiguration.additionalIrCheckers = IrCheckersEnabledByTestDirectives.filter { module.directives.contains(it.key) }.values.toList()

                    val key = ConfigurationKey(kind, jdkKind, keyConfiguration.toString())
                    val compiler = if (kind.withReflection) reflectionFlavor else commonFlavor
                    val compilerConfigurationProvider = services.compilerConfigurationProvider as CompilerConfigurationProviderImpl
                    val filesHolder = holders.getOrPut(key) {
                        FilesWriter(
                            compiler,
                            compilerConfigurationProvider.createCompilerConfiguration(module, CompilationStage.FIRST),
                        ).also {
                            println("Creating new configuration by $key")
                        }
                    }

                    if (testConfiguration.metaTestConfigurators.any { it.shouldSkipTest() }) {
                        continue
                    }

                    patchFilesAndAddTest(file, module, services, filesHolder)
                }
            }
        }
    }

    private fun createTestConfiguration(testDataFile: File): NonGroupingStageTestConfiguration {
        return TestConfigurationBuilder().apply {
            configure()
            testInfo = KotlinTestInfo(
                "org.jetbrains.kotlin.android.tests.AndroidRunner",
                "test${testDataFile.nameWithoutExtension.replaceFirstChar(Char::uppercaseChar)}",
                emptySet()
            )
            startingArtifactFactory = { ResultingArtifact.Source() }
            @OptIn(TestInfrastructureInternals::class)
            useCustomCompilerConfigurationProvider(::CompilerConfigurationProviderImpl)
        }.build(testDataFile.path)
    }

    private fun TestConfigurationBuilder.configure() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetBackend = TargetBackend.ANDROID
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
        )

        assertions = JUnit5Assertions
        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        useAdditionalService<TargetPlatformProvider>(::TargetPlatformProviderForCompilerTests)
        useAdditionalService<ApplicationDisposableProvider> { ExecutionListenerBasedDisposableProvider() }
        useAdditionalService<KotlinStandardLibrariesPathProvider> { StandardLibrariesPathProviderForKotlinProject }
        useSourcePreprocessor(*AbstractKotlinCompilerTest.defaultPreprocessors.toTypedArray())
        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())
        useDirectives(CodegenTestDirectives)
        class AndroidTransformingPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
            override fun process(file: TestFile, content: String): String {
                val transformers = Android.forAll + (Android.forSpecificFile[file.originalFile]?.let { listOf(it) } ?: emptyList())
                return transformers.fold(content) { text, transformer -> transformer(text) }
            }
        }
        useSourcePreprocessor({ AndroidTransformingPreprocessor(it) }, ::JvmInlineSourceTransformer)
        useMetaTestConfigurators(::ClassicUnstableAndK2LanguageFeaturesSkipConfigurator)
    }

    companion object {
        const val GRADLE_VERSION = "8.14" // update GRADLE_SHA_256 on change
        const val GRADLE_SHA_256 = "61ad310d3c7d3e5da131b76bbf22b5a4c0786e9d892dae8c1658d4b484de3caa"
        const val testClassPackage = "org.jetbrains.kotlin.android.tests"


        @JvmOverloads
        @JvmStatic
        @Throws(Throwable::class)
        fun generate(pathManager: PathManager, skipSdkDirWriting: Boolean = false) {
            CodegenTestsOnAndroidGenerator(pathManager).prepareAndroidModuleAndGenerateTests(skipSdkDirWriting)
        }

        @JvmOverloads
        @JvmStatic
        @Throws(Throwable::class)
        internal fun discover(pathManager: PathManager, skipSdkDirWriting: Boolean = false): AndroidTestPlan {
            return CodegenTestsOnAndroidGenerator(pathManager).prepareAndroidModuleAndDiscoverTests(skipSdkDirWriting)
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
            val pathManager = PathManager(tmpFolder)
            generate(pathManager, true)
            println("Android test project is generated into $tmpFolder folder")
        }
    }
}
