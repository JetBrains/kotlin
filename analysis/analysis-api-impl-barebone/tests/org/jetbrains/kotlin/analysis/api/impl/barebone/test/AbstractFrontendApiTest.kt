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
import com.intellij.testFramework.TestDataFile
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
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
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import java.util.concurrent.ExecutionException
import kotlin.io.path.nameWithoutExtension

interface FrontendApiTestConfiguratorService {
    val testPrefix: String?
        get() = null

    val allowDependedAnalysisSession: Boolean
        get() = true

    fun TestConfigurationBuilder.configureTest(disposable: Disposable)

    fun processTestFiles(files: List<KtFile>): List<KtFile> = files
    fun getOriginalFile(file: KtFile): KtFile = file

    fun registerProjectServices(project: MockProject)
    fun registerApplicationServices(application: MockApplication)

    fun prepareTestFiles(files: List<KtFile>, module: TestModule, testServices: TestServices) {}

    fun doOutOfBlockModification(file: KtFile)
}

abstract class AbstractFrontendApiTest(val configurator: FrontendApiTestConfiguratorService) : TestWithDisposable() {
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

    private fun getTestDataFileSiblingPath(extension: String, testPrefix: String?): Path {
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
        configureTest(this)

        startingArtifactFactory = { ResultingArtifact.Source() }
        this.testInfo = this@AbstractFrontendApiTest.testInfo
    }

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
            configurator.registerProjectServices(this)
        }

        registerApplicationServices()

        val ktFiles = moduleInfo.testFilesToKtFiles.filterKeys { testFile -> !testFile.isAdditional }.values.toList()
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