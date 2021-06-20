/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.test.framework

import com.intellij.mock.MockProject
import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.fir.session.FirModuleInfoBasedModuleData
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.FirModuleResolveStateConfiguratorForSingleModuleTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.TestModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.registerTestServices
import org.jetbrains.kotlin.idea.fir.test.framework.*
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.frontend.fir.FirModuleInfoProvider
import org.jetbrains.kotlin.test.frontend.fir.firModuleInfoProvider
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension

abstract class AbstractKtIdeaTest {
    private lateinit var testInfo: KotlinTestInfo

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

        useSourcePreprocessor(::ExpressionMarkersSourceFilePreprocessor)
        useAdditionalService(::ExpressionMarkerProvider)
        useAdditionalService(::FirModuleInfoProvider)

        configureTest(this)

        this.testInfo = this@AbstractKtIdeaTest.testInfo
    }

    protected lateinit var testDataPath: Path

    protected fun testDataFileSibling(extension: String): Path {
        val extensionWithDot = "." + extension.removePrefix(".")
        return testDataPath.resolveSibling(testDataPath.nameWithoutExtension + extensionWithDot)
    }

    open fun configureTest(builder: TestConfigurationBuilder) {}


    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    protected fun runTest(path: String) {
        testDataPath = Paths.get(path)
        val testConfiguration = testConfiguration(path, configure)
        val testServices = testConfiguration.testServices
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            path,
            testConfiguration.directives,
        ).also {
            testConfiguration.testServices.register(TestModuleStructure::class, it)
        }

        val singleModule = moduleStructure.modules.single()
        val project = testServices.compilerConfigurationProvider.getProject(singleModule)
        val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(singleModule.files, project)

        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        val moduleInfo = TestModuleInfo(singleModule)
        testServices.firModuleInfoProvider.registerModuleData(singleModule, FirModuleInfoBasedModuleData(moduleInfo))
        val configurator = FirModuleResolveStateConfiguratorForSingleModuleTestImpl(testServices, singleModule, ktFiles, moduleInfo)

        with(project as MockProject) {
            registerTestServices(configurator, ktFiles)
            registerService(KtAnalysisSessionProvider::class.java, KtFirAnalysisSessionProvider::class.java)
        }

        doTestByFileStructure(ktFiles.values.toList(), moduleStructure, testServices)
    }

    abstract fun doTestByFileStructure(ktFiles: List<KtFile>, moduleStructure: TestModuleStructure, testServices: TestServices)

    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        this.testInfo = KotlinTestInfo(
            className = testInfo.testClass.orElseGet(null)?.name ?: "_undefined_",
            methodName = testInfo.testMethod.orElseGet(null)?.name ?: "_testUndefined_",
            tags = testInfo.tags
        )
    }

    protected fun getCaretPosition(text: String) = text.indexOfOrNull(KtTest.CARET_SYMBOL)
}


fun String.indexOfOrNull(substring: String) =
    indexOf(substring).takeIf { it >= 0 }

private fun String.indexOfOrNull(substring: String, startingIndex: Int) =
    indexOf(substring, startingIndex).takeIf { it >= 0 }