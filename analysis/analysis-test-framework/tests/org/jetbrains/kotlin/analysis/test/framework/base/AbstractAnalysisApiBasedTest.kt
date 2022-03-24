/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.TestDataFile
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.analyseInDependedAnalysisSession
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.TestWithDisposable
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KotlinProjectStructureProviderTestImpl
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestKtModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor
import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtFilesFromModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement
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
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

abstract class AbstractAnalysisApiBasedTest : TestWithDisposable() {
    abstract val configurator: AnalysisApiTestConfiguratorService

    protected lateinit var testInfo: KotlinTestInfo
        private set

    protected lateinit var testDataPath: Path
        private set

    private lateinit var moduleStructure: TestModuleStructure
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
        useAdditionalService<KotlinStandardLibrariesPathProvider> { StandardLibrariesPathProviderForKotlinProject }
        useDirectives(AnalysisApiTestDirectives)
        configureTest(this)

        startingArtifactFactory = { ResultingArtifact.Source() }
        this.testInfo = this@AbstractAnalysisApiBasedTest.testInfo
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
        this.moduleStructure = moduleStructure
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
            val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(singleModule)
            compilerConfiguration.addJavaSourceRoots(ktFiles.map { File(it.virtualFilePath) })
            configurator.registerProjectServices(
                this,
                compilerConfiguration,
                ktFiles,
                testServices.compilerConfigurationProvider.getPackagePartProviderFactory(singleModule),
                KotlinProjectStructureProviderTestImpl(testServices)
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
    }

    protected fun <R> analyseForTest(contextElement: KtElement, action: KtAnalysisSession.() -> R): R {
        return if (configurator.analyseInDependentSession
            && AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE !in this.moduleStructure.allDirectives
        ) {
            val originalContainingFile = contextElement.containingKtFile
            val fileCopy = originalContainingFile.copy() as KtFile
            analyseInDependedAnalysisSession(originalContainingFile, PsiTreeUtil.findSameElementInCopy(contextElement, fileCopy), action)
        } else {
            analyse(contextElement, action)
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
}