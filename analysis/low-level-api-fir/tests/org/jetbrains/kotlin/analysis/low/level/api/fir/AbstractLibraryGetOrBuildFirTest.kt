/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.service

abstract class AbstractLibraryGetOrBuildFirTest : AbstractLowLevelApiSingleFileTest() {
    override val configurator = AnalysisApiFirLibraryBinaryTestConfigurator
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            useAdditionalServices(service(::CompiledLibraryProvider))
        }
    }

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val declaration = getElementToSearch(ktFile, moduleStructure)

        val module = ProjectStructureProvider.getModule(ktFile.project, ktFile, contextualModule = null)
        val resolveSession = LLFirResolveSessionService.getInstance(ktFile.project).getFirResolveSessionForBinaryModule(module)
        val symbolProvider = resolveSession.getSessionFor(module).symbolProvider
        val fir = FirDeclarationForCompiledElementSearcher(symbolProvider).findNonLocalDeclaration(declaration)

        testServices.assertions.assertEqualsToTestDataFileSibling(renderActualFir(fir, declaration, true))
    }

    private fun getElementToSearch(ktFile: KtFile, moduleStructure: TestModuleStructure): KtDeclaration {
        val expectedType = moduleStructure.allDirectives[Directives.DECLARATION_TYPE].firstOrNull()
            ?: error("Compiled code should have element type specified")
        @Suppress("UNCHECKED_CAST") val expectedClass = Class.forName(expectedType) as Class<PsiElement>
        return findFirstDeclaration(ktFile.declarations, expectedClass)!!
    }

    private fun findFirstDeclaration(
        declarations: List<KtDeclaration>,
        expectedClass: Class<PsiElement>
    ): KtDeclaration? {
        declarations.filterIsInstance(expectedClass).firstOrNull()?.let { return it as KtDeclaration }
        declarations.forEach { decl ->
            if (decl is KtDeclarationContainer) {
                findFirstDeclaration(decl.declarations, expectedClass)?.let { return it }
            }
            if (decl is KtFunction) {
                findFirstDeclaration(decl.valueParameters, expectedClass)?.let { return it }
            }
            if (decl is KtClass && expectedClass == KtConstructor::class.java) {
                decl.primaryConstructor?.let { return it }
            }
        }
        return null
    }

    private object Directives : SimpleDirectivesContainer() {
        val DECLARATION_TYPE by stringDirective("DECLARATION_TYPE")
    }
}