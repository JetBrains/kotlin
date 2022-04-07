/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.barebone.test

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.TestDataFile
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ExecutionListenerBasedDisposableProvider
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

interface FrontendApiTestConfiguratorService {
    val testPrefix: String?
        get() = null

    val allowDependedAnalysisSession: Boolean
        get() = true

    fun TestConfigurationBuilder.configureTest(disposable: Disposable)

    fun processTestFiles(files: List<KtFile>): List<KtFile> = files

    fun getOriginalFile(file: KtFile): KtFile = file

    fun registerProjectServices(
        project: MockProject,
        compilerConfig: CompilerConfiguration,
        files: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectStructureProvider: ProjectStructureProvider,
        jarFileSystem: CoreJarFileSystem,
    )

    fun registerApplicationServices(application: MockApplication)

    fun prepareTestFiles(files: List<KtFile>, module: TestModule, testServices: TestServices) {}

    fun doOutOfBlockModification(file: KtFile)

    fun preprocessTestDataPath(path: Path): Path = path
}

abstract class AbstractFrontendApiTest : TestWithDisposable() {
    abstract val configurator: FrontendApiTestConfiguratorService

    protected open val enableTestInDependedMode: Boolean
        get() = configurator.allowDependedAnalysisSession

    protected lateinit var testInfo: KotlinTestInfo
        private set

    protected var useDependedAnalysisSession: Boolean = false

    protected lateinit var testDataPath: Path
        private set

    protected open fun configureTest(builder: TestConfigurationBuilder) {
        with(configurator) {
            builder.configureTest(disposable)
        }
    }

    protected abstract fun doTestByFileStructure(ktFiles: List<KtFile>, moduleStructure: TestModuleStructure, testServices: TestServices)

    protected fun AssertionsService.assertEqualsToTestDataFileSibling(actual: String, extension: String = ".txt") {
        val testPrefix = configurator.testPrefix

        val expectedFile = getTestDataFileSiblingPath(extension, testPrefix = testPrefix)
        assertEqualsToFile(expectedFile, actual)

        if (testPrefix != null) {
            val expectedFileWithoutPrefix = getTestDataFileSiblingPath(extension, testPrefix = null)
            if (expectedFile != expectedFileWithoutPrefix) {
                try {
                    assertEqualsToFile(expectedFileWithoutPrefix, actual)
                } catch (ignored: ComparisonFailure) {
                    return
                }

                throw AssertionError("\"$expectedFile\" has the same content as \"$expectedFileWithoutPrefix\". Delete the prefixed file.")
            }
        }
    }

    protected fun getTestDataFileSiblingPath(extension: String, testPrefix: String?): Path {
        val extensionWithDot = "." + extension.removePrefix(".")
        val baseName = testDataPath.nameWithoutExtension

        if (testPrefix != null) {
            val prefixedFile = testDataPath.resolveSibling("$baseName.$testPrefix$extensionWithDot")
            if (prefixedFile.exists()) {
                return prefixedFile
            }
        }

        return testDataPath.resolveSibling(baseName + extensionWithDot)
    }

    @OptIn(TestInfrastructureInternals::class)
    private val configure: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )
        assertions = JUnit5Assertions
        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)

        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())
        useDirectives(JvmEnvironmentConfigurationDirectives)

        useSourcePreprocessor(::ExpressionMarkersSourceFilePreprocessor)
        useAdditionalService { ExpressionMarkerProvider() }
        useAdditionalService(::TestKtModuleProvider)
        useAdditionalService<ApplicationDisposableProvider> { ExecutionListenerBasedDisposableProvider() }
        configureTest(this)

        startingArtifactFactory = { ResultingArtifact.Source() }
        this.testInfo = this@AbstractFrontendApiTest.testInfo
    }

    protected open fun handleInitializationError(exception: Throwable, moduleStructure: TestModuleStructure): InitializationErrorAction =
        InitializationErrorAction.THROW

    enum class InitializationErrorAction {
        IGNORE, THROW
    }

    protected fun runTest(@TestDataFile path: String) {
        testDataPath = configurator.preprocessTestDataPath(Paths.get(path))
        val testConfiguration = testConfiguration(path, configure)
        Disposer.register(disposable, testConfiguration.rootDisposable)
        val testServices = testConfiguration.testServices
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            path,
            testConfiguration.directives,
        )
        val singleModule = moduleStructure.modules.single()
        val project = try {
            testServices.compilerConfigurationProvider.getProject(singleModule)
        } catch (_: SkipTestException) {
            return
        }

        registerApplicationServices()
        testConfiguration.testServices.register(TestModuleStructure::class, moduleStructure)
        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            try {
                preprocessor.preprocessModuleStructure(moduleStructure)
            } catch (exception: Throwable) {
                when (handleInitializationError(exception, moduleStructure)) {
                    InitializationErrorAction.IGNORE -> {}
                    InitializationErrorAction.THROW -> throw exception
                }
            }
        }

        val ktFiles = getKtFilesFromModule(testServices, singleModule)
        with(project as MockProject) {
            val compilerConfigurationProvider = testServices.compilerConfigurationProvider
            val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(singleModule)
            compilerConfiguration.addJavaSourceRoots(ktFiles.map { File(it.virtualFilePath) })
            configurator.registerProjectServices(
                this,
                compilerConfiguration,
                ktFiles,
                compilerConfigurationProvider.getPackagePartProviderFactory(singleModule),
                KotlinProjectStructureProviderTestImpl(testServices),
                compilerConfigurationProvider.getJarFileSystem(singleModule)
            )
        }

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            try {
                preprocessor.prepareSealedClassInheritors(moduleStructure)
            } catch (exception: Throwable) {
                when (handleInitializationError(exception, moduleStructure)) {
                    InitializationErrorAction.IGNORE -> {}
                    InitializationErrorAction.THROW -> throw exception
                }
            }
        }

        configurator.prepareTestFiles(ktFiles, singleModule, testServices)
        doTestByFileStructure(ktFiles, moduleStructure, testServices)
        if (!enableTestInDependedMode || ktFiles.any {
                InTextDirectivesUtils.isDirectiveDefined(it.text, DISABLE_DEPENDED_MODE_DIRECTIVE)
            }) {
            return
        }
        try {
            useDependedAnalysisSession = true
            doTestByFileStructure(configurator.processTestFiles(ktFiles), moduleStructure, testServices)
        } catch (e: SkipDependedModeException) {
            // Skip the test if needed
        } catch (e: ExecutionException) {
            if (e.cause !is SkipDependedModeException)
                throw Exception("Test succeeded in normal analysis mode but failed in depended analysis mode.", e)
        } catch (e: Exception) {
            throw Exception("Test succeeded in normal analysis mode but failed in depended analysis mode.", e)
        }
    }

    private fun registerApplicationServices() {
        val application = ApplicationManager.getApplication() as MockApplication
        KotlinCoreEnvironment.underApplicationLock {
            configurator.registerApplicationServices(application)
        }
    }

    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        this.testInfo = KotlinTestInfo(
            className = testInfo.testClass.orElseGet(null)?.name ?: "_undefined_",
            methodName = testInfo.testMethod.orElseGet(null)?.name ?: "_testUndefined_",
            tags = testInfo.tags
        )
    }

    protected class SkipDependedModeException : Exception()

    companion object {
        val DISABLE_DEPENDED_MODE_DIRECTIVE = "DISABLE_DEPENDED_MODE"
    }
}

fun getKtFilesFromModule(testServices: TestServices, testModule: TestModule): List<KtFile> {
    val moduleInfoProvider = testServices.projectModuleProvider
    return when (val moduleInfo = moduleInfoProvider.getModule(testModule.name)) {
        is TestKtSourceModule -> moduleInfo.testFilesToKtFiles.filterKeys { testFile -> !testFile.isAdditional }.values.toList()
        is TestKtLibraryModule -> moduleInfo.ktFiles.toList()
        is TestKtLibrarySourceModule -> moduleInfo.ktFiles.toList()
        else -> error("Unexpected $moduleInfo")
    }
}
