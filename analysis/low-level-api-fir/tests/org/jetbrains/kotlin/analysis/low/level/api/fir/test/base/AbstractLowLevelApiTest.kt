/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.analyseInDependedAnalysisSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.ModuleRegistrarPreAnalysisHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.TestKtModuleProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.projectModuleProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalKtFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.bind
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
import java.util.concurrent.ExecutionException
import kotlin.io.path.nameWithoutExtension

abstract class AbstractLowLevelApiTest : TestWithDisposable() {
    private lateinit var testInfo: KotlinTestInfo
    private var useDependedAnalysisSession: Boolean = false
    open protected val enableTestInDependedMode: Boolean = true

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
        usePreAnalysisHandlers(::ModuleRegistrarPreAnalysisHandler.bind(disposable))
        configureTest(this)

        startingArtifactFactory = { ResultingArtifact.Source() }
        this.testInfo = this@AbstractLowLevelApiTest.testInfo
    }

    protected lateinit var testDataPath: Path

    protected fun testDataFileSibling(extension: String): Path {
        val extensionWithDot = "." + extension.removePrefix(".")
        return testDataPath.resolveSibling(testDataPath.nameWithoutExtension + extensionWithDot)
    }

    open fun configureTest(builder: TestConfigurationBuilder) {}

    protected fun runTest(@TestDataFile path: String) {
        testDataPath = Paths.get(path)
        val testConfiguration = testConfiguration(path, configure)
        Disposer.register(disposable, testConfiguration.rootDisposable)
        val testServices = testConfiguration.testServices
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            path,
            testConfiguration.directives,
        ).also { testModuleStructure ->
            testConfiguration.testServices.register(TestModuleStructure::class, testModuleStructure)
            testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
                preprocessor.preprocessModuleStructure(testModuleStructure)
            }
        }


        val singleModule = moduleStructure.modules.single()
        val project = testServices.compilerConfigurationProvider.getProject(singleModule)
        val moduleInfoProvider = testServices.projectModuleProvider

        val moduleInfo = moduleInfoProvider.getModule(singleModule.name)

        with(project as MockProject) {
            registerServicesForProject(this)
        }

        registerApplicationServices()

        val ktFiles = moduleInfo.testFilesToKtFiles.filterKeys { testFile -> !testFile.isAdditional }.values.toList()

        doTestByFileStructure(ktFiles, moduleStructure, testServices)
        if (!enableTestInDependedMode || ktFiles.any {
                InTextDirectivesUtils.isDirectiveDefined(it.text, DISABLE_DEPENDED_MODE_DIRECTIVE)
            }) {
            return
        }
        try {
            useDependedAnalysisSession = true
            doTestByFileStructure(ktFiles.map {
                val fakeFile = it.copy() as KtFile
                fakeFile.originalKtFile = it
                fakeFile
            }, moduleStructure, testServices)
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
            registerApplicationServices(application)
        }
    }

    protected open fun registerServicesForProject(project: MockProject) {}

    protected open fun registerApplicationServices(application: MockApplication) {}

    protected abstract fun doTestByFileStructure(ktFiles: List<KtFile>, moduleStructure: TestModuleStructure, testServices: TestServices)

    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        this.testInfo = KotlinTestInfo(
            className = testInfo.testClass.orElseGet(null)?.name ?: "_undefined_",
            methodName = testInfo.testMethod.orElseGet(null)?.name ?: "_testUndefined_",
            tags = testInfo.tags
        )
    }

    protected inline fun <R> analyseOnPooledThreadInReadAction(context: KtElement, crossinline action: KtAnalysisSession.() -> R): R =
        executeOnPooledThreadInReadAction {
            analyseForTest(context) { action() }
        }

    protected inline fun <T> runReadAction(crossinline runnable: () -> T): T {
        return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
    }

    protected inline fun <R> executeOnPooledThreadInReadAction(crossinline action: () -> R): R =
        ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()


    protected fun <R> analyseForTest(contextElement: KtElement, action: KtAnalysisSession.() -> R): R {
        return if (useDependedAnalysisSession) {
            // Depended mode does not support analysing a KtFile. See org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir#getResolveStateForDependentCopy
            if (contextElement is KtFile) throw SkipDependedModeException()

            require(!contextElement.isPhysical)
            analyseInDependedAnalysisSession(contextElement.containingKtFile.originalKtFile!!, contextElement, action)
        } else {
            analyse(contextElement, action)
        }
    }

    private class SkipDependedModeException : Exception()

    companion object {
        val DISABLE_DEPENDED_MODE_DIRECTIVE = "DISABLE_DEPENDED_MODE"
    }
}

