/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.TestDataFile
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.analyseInDependedAnalysisSession
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.TestWithDisposable
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.LibraryWasNotCompiledDueToExpectedCompilationError
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ExecutionListenerBasedDisposableProvider
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TestConfiguration
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
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

abstract class AbstractAnalysisApiBasedTest : TestWithDisposable() {
    abstract val configurator: AnalysisApiTestConfigurator

    private lateinit var testInfo: KotlinTestInfo

    protected lateinit var testDataPath: Path
        private set

    private lateinit var testServices: TestServices

    protected open fun configureTest(builder: TestConfigurationBuilder) {
        configurator.configureTest(builder, disposable)
    }

    protected abstract fun doTestByFileStructure(moduleStructure: TestModuleStructure, testServices: TestServices)

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

        registerAnalysisApiBaseTestServices(disposable, configurator)
        configureTest(this)

        startingArtifactFactory = { ResultingArtifact.Source() }
        this.testInfo = this@AbstractAnalysisApiBasedTest.testInfo
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    protected fun runTest(@TestDataFile path: String) {
        testDataPath = configurator.preprocessTestDataPath(Paths.get(path))
        val testConfiguration = createTestConfiguration()
        testServices = testConfiguration.testServices
        val moduleStructure = createModuleStructure(testConfiguration)

        try {
            prepareToTheAnalysis(testConfiguration)
        } catch (ignored: SkipTestException) {
            return
        }

        if (configurator.analyseInDependentSession && isDependentModeDisabledForTheTest()) {
            return
        }

        doTestByFileStructure(moduleStructure, testServices)
    }

    private fun createTestConfiguration(): TestConfiguration {
        val testConfiguration = testConfiguration(testDataPath.toString(), configure)
        Disposer.register(disposable, testConfiguration.rootDisposable)
        return testConfiguration
    }

    private fun createModuleStructure(testConfiguration: TestConfiguration): TestModuleStructure {
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(testDataPath.toString(), testConfiguration.directives)
        testServices.register(TestModuleStructure::class, moduleStructure)
        return moduleStructure
    }

    private fun prepareToTheAnalysis(testConfiguration: TestConfiguration) {
        val moduleStructure = testServices.moduleStructure
        testConfiguration.preAnalysisHandlers.forEach { preprocessor -> preprocessor.preprocessModuleStructure(moduleStructure) }
        testConfiguration.preAnalysisHandlers.forEach { preprocessor -> preprocessor.prepareSealedClassInheritors(moduleStructure) }

        moduleStructure.modules.forEach { module ->
            val files = testServices.ktModuleProvider.getModuleFiles(module)
            configurator.prepareFilesInModule(files, module, testServices)
        }
    }

    private fun isDependentModeDisabledForTheTest(): Boolean =
        AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE in testServices.moduleStructure.allDirectives

    protected fun <R> analyseForTest(contextElement: KtElement, action: KtAnalysisSession.() -> R): R {
        return if (configurator.analyseInDependentSession) {
            val originalContainingFile = contextElement.containingKtFile
            val fileCopy = originalContainingFile.copy() as KtFile
            analyseInDependedAnalysisSession(originalContainingFile, PsiTreeUtil.findSameElementInCopy(contextElement, fileCopy), action)
        } else {
            analyse(contextElement, action)
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