/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.mock.MockProject
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.RendererModifier
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.impl.base.test.TestReferenceResolveResultRenderer.renderResolvedTo
import org.jetbrains.kotlin.analysis.api.impl.base.test.findReferencesAtCaret
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleModuleTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import java.io.File

abstract class AbstractStandaloneModeSingleModuleTest : AbstractHLApiSingleModuleTest() {

    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val project = testServices.compilerConfigurationProvider.getProject(module) as MockProject
        // (project.picoContainer as DefaultPicoContainer).release() // is way too strong :( even PsiManager is gone
        unregisterServices(project)
        // TODO: better to be part of `StandaloneModeUtilsConfiguratorService`
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        compilerConfiguration.addJavaSourceRoots(ktFiles.map { File(it.virtualFilePath) })
        configureProjectEnvironment(
            project,
            compilerConfiguration,
            ktFiles,
            testServices.compilerConfigurationProvider.getPackagePartProviderFactory(module)
        )

        val mainKtFile = ktFiles.singleOrNull() ?: ktFiles.first { it.name == "main.kt" }
        val caretPosition = testServices.expressionMarkerProvider.getCaretPosition(mainKtFile)
        val ktReferences = findReferencesAtCaret(mainKtFile, caretPosition)
        if (ktReferences.isEmpty()) {
            testServices.assertions.fail { "No references at caret found" }
        }

        val resolvedTo =
            analyseForTest(
                PsiTreeUtil.findElementOfClassAtOffset(mainKtFile, caretPosition, KtDeclaration::class.java, false) ?: mainKtFile
            ) {
                val symbols = ktReferences.flatMap { it.resolveToSymbols() }
                renderResolvedTo(symbols, renderingOptions)
            }

        val actual = "Resolved to:\n$resolvedTo"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private val renderingOptions = KtDeclarationRendererOptions.DEFAULT.copy(
        modifiers = RendererModifier.DEFAULT - RendererModifier.ANNOTATIONS,
        sortNestedDeclarations = true
    )

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    private fun unregisterServices(project: MockProject) {
        project.picoContainer.unregisterComponent(KtAnalysisSessionProvider::class.qualifiedName)
        project.picoContainer.unregisterComponent(KotlinModificationTrackerFactory::class.qualifiedName)
        RegisterComponentService.unregisterLLFirResolveStateService(project)
        project.picoContainer.unregisterComponent(FirSealedClassInheritorsProcessorFactory::class.qualifiedName)
        project.picoContainer.unregisterComponent(KtModuleScopeProvider::class.qualifiedName)
        project.picoContainer.unregisterComponent(ProjectStructureProvider::class.qualifiedName)
        project.picoContainer.unregisterComponent(KotlinDeclarationProviderFactory::class.qualifiedName)
        project.picoContainer.unregisterComponent(KotlinPackageProviderFactory::class.qualifiedName)
        project.picoContainer.unregisterComponent(PackagePartProviderFactory::class.qualifiedName)
    }
}
