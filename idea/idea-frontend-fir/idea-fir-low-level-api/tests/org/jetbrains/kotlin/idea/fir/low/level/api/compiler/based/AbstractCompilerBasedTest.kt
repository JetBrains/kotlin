/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.impl.ProjectExImpl
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.serviceContainer.ComponentManagerImpl
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.MockExternalAnnotationsManager
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getFirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.createResolveStateForNoCaching
import org.jetbrains.kotlin.idea.project.withLanguageVersionSettings
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.getLanguageLevel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.TestRunner
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

abstract class AbstractCompilerBasedTest : KotlinLightCodeInsightFixtureTestCase() {
    private var _configuration: TestConfiguration? = null
    private var _modules: TestModuleStructure? = null

    protected val modules: TestModuleStructure get() = _modules!!
    protected val configuration: TestConfiguration get() = _configuration!!

    private var oldAnnoatationManager: ExternalAnnotationsManager? = null

    private fun <R> withLanguageLevel(directives: RegisteredDirectives, action: () -> R): R {
        val oldJavaLanguageLevel = LanguageLevelProjectExtension.getInstance(project).languageLevel
        val requiredLanguageLevel = getLanguageLevel(JvmEnvironmentConfigurator.extractJdkKind(directives))
        LanguageLevelProjectExtension.getInstance(project).languageLevel = requiredLanguageLevel
        return try {
            action()
        } finally {
            LanguageLevelProjectExtension.getInstance(project).languageLevel = oldJavaLanguageLevel
        }
    }

    inner class LowLevelFirFrontendFacade(
        testServices: TestServices
    ) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
        override val additionalDirectives: List<DirectivesContainer>
            get() = listOf(FirDiagnosticsDirectives)

        override fun analyze(module: TestModule): LowLevelFirOutputArtifact {
            val files = module.files.associateWith { file ->
                val text = testServices.sourceFileProvider.getContentOfSourceFile(file)
                myFixture.addFileToProject(file.relativePath, text)
            }
            val ktFile = files.values.firstIsInstance<KtFile>()
            val moduleInfo = ktFile.getModuleInfo()
            val resolveState = createResolveState(moduleInfo, module.languageVersionSettings)
            val allFirFiles = files.mapNotNull { (testFile, psiFile) ->
                if (psiFile !is KtFile) return@mapNotNull null
                testFile to psiFile.getFirFile(resolveState)
            }.toMap()

            val diagnosticCheckerFilter = if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                DiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS
            } else DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS

            val analyzerFacade = LowLevelFirAnalyzerFacade(resolveState, allFirFiles, diagnosticCheckerFilter)
            return LowLevelFirOutputArtifact(resolveState.rootModuleSession, analyzerFacade)
        }
    }

    private fun createResolveState(moduleInfo: IdeaModuleInfo, languageVersionSettings: LanguageVersionSettings): FirModuleResolveState {
        require(moduleInfo is ModuleSourceInfo)
        var resolveState: FirModuleResolveState? = null
        moduleInfo.module.withLanguageVersionSettings(languageVersionSettings) {
            resolveState = createResolveStateForNoCaching(moduleInfo)
        }
        return resolveState!!
    }

    override fun isFirPlugin(): Boolean = true


    override fun setUp() {
        if (!isAllFilesPresentInTest()) {
            setupConfiguration()
        }
        super.setUp()
        registerMockExternalAnnotationManager()
    }

    private fun registerMockExternalAnnotationManager() {
        oldAnnoatationManager = project.serviceIfCreated()
        registerAnnotationManager(MockExternalAnnotationsManager())
    }

    private fun unregisterMockExternalAnnotationManager() {
        oldAnnoatationManager?.let { old ->
            registerAnnotationManager(old)
        }
    }

    private fun registerAnnotationManager(manager: ExternalAnnotationsManager) {
        @Suppress("UnstableApiUsage") val projectExImpl = project as ProjectExImpl
        projectExImpl.registerServiceInstance(
            ExternalAnnotationsManager::class.java,
            manager,
            ComponentManagerImpl.fakeCorePluginDescriptor
        )
    }

    private fun setupConfiguration() {
        _configuration = testConfiguration(testPath()) {
            testInfo = KotlinTestInfo(
                className = "_undefined_",
                methodName = "_testUndefined_",
                tags = emptySet()
            )
            configureTest()
            AbstractKotlinCompilerTest.defaultConfiguration(this)
            unregisterAllFacades()
            useFrontendFacades(::LowLevelFirFrontendFacade)
            assertions = JUnit4AssertionsService
        }
        _modules = configuration.moduleStructureExtractor.splitTestDataByModules(testPath(), configuration.directives)
    }

    override fun tearDown() {
        _configuration = null
        _modules = null
        unregisterMockExternalAnnotationManager()
        super.tearDown()
    }

    abstract fun TestConfigurationBuilder.configureTest()


    fun doTest(path: String) {
        if (ignoreTest()) return
        withLanguageLevel(modules.allDirectives) {
            TestRunner(configuration).runTest(path)
        }
    }

    private fun ignoreTest(): Boolean {
        if (modules.modules.size > 1) {
            return true // multimodule tests are not supported
        }

        val singleModule = modules.modules.single()
        if (singleModule.files.none { it.isKtFile }) {
            return true // nothing to highlight
        }

        return false
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (isAllFilesPresentInTest()) return KotlinLightProjectDescriptor.INSTANCE

        val allDirectives = modules.allDirectives

        val configurationKind = JvmEnvironmentConfigurator.extractConfigurationKind(allDirectives)
        val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(allDirectives)

        @OptIn(ExperimentalStdlibApi::class)
        val libraryFiles = buildList {
            if (configurationKind.withRuntime) {
                add(ForTestCompileRuntime.runtimeJarForTests())
                add(ForTestCompileRuntime.scriptRuntimeJarForTests())
            }
            addAll(JvmEnvironmentConfigurator.getLibraryFilesExceptRealRuntime(configurationKind, allDirectives))
        }

        return object : KotlinJdkAndLibraryProjectDescriptor(libraryFiles) {
            override fun getSdk(): Sdk = PluginTestCaseBase.jdk(jdkKind)
        }
    }
}