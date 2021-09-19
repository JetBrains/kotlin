/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.ModuleRegistrarPreAnalysisHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.TestKtModuleProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.projectModuleProvider
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
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
import kotlin.io.path.nameWithoutExtension

abstract class AbstractLowLevelApiTest : TestWithDisposable() {
    private lateinit var testInfo: KotlinTestInfo

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

        doTestByFileStructure(
            moduleInfo.testFilesToKtFiles.filter { (testFile, _) -> !testFile.isAdditional }.values.toList(),
            moduleStructure,
            testServices
        )
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
}

