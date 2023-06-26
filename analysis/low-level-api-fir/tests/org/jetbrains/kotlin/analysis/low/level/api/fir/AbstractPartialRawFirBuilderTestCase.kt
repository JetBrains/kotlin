/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import junit.framework.TestCase
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.RawFirNonLocalDeclarationBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.renderer.ConeIdFullRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.io.path.readText

abstract class AbstractPartialRawFirBuilderTestCase : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val fileText = testDataPath.readText()
        val functionName = InTextDirectivesUtils.findStringWithPrefixes(fileText, FUNCTION_DIRECTIVE)
        val propertyName = InTextDirectivesUtils.findStringWithPrefixes(fileText, PROPERTY_DIRECTIVE)

        when {
            functionName != null -> testFunctionPartialBuilding(ktFile, functionName)
            propertyName != null -> testPropertyPartialBuilding(ktFile, propertyName)
            else -> testServices.assertions.fail { "No '$FUNCTION_DIRECTIVE' or '$PROPERTY_DIRECTIVE' directives found!" }
        }
    }

    private fun testFunctionPartialBuilding(ktFile: KtFile, nameToFind: String) {
        testPartialBuilding(
            ktFile
        ) { file -> file.findDescendantOfType<KtNamedFunction> { it.name == nameToFind }!! }
    }

    private fun testPropertyPartialBuilding(ktFile: KtFile, nameToFind: String) {
        testPartialBuilding(
            ktFile
        ) { file -> file.findDescendantOfType<KtProperty> { it.name == nameToFind }!! }
    }

    private class DesignationBuilder(private val elementToBuild: KtDeclaration) : FirVisitorVoid() {
        private val path = mutableListOf<FirRegularClass>()
        var resultDesignation: FirDesignation? = null
            private set

        override fun visitElement(element: FirElement) {
            if (resultDesignation != null) return
            when (element) {
                is FirSimpleFunction, is FirProperty -> {
                    if (element.psi == elementToBuild) {
                        val originalDeclaration = element as FirDeclaration
                        resultDesignation = FirDesignation(path, originalDeclaration)
                    } else {
                        element.acceptChildren(this)
                    }
                }
                is FirRegularClass -> {
                    path.add(element)
                    element.acceptChildren(this)
                    if (resultDesignation == null) {
                        path.removeLast()
                    }
                }
                else -> {
                    element.acceptChildren(this)
                }
            }
        }
    }


    private fun <T : KtElement> testPartialBuilding(
        file: KtFile,
        findPsiElement: (KtFile) -> T
    ) {
        val elementToBuild = findPsiElement(file) as KtDeclaration

        val scopeProvider = object : FirScopeProvider() {
            override fun getUseSiteMemberScope(
                klass: FirClass,
                useSiteSession: FirSession,
                scopeSession: ScopeSession,
                memberRequiredPhase: FirResolvePhase?,
            ): FirTypeScope = error("Should not be called")

            override fun getStaticMemberScopeForCallables(
                klass: FirClass,
                useSiteSession: FirSession,
                scopeSession: ScopeSession,
            ): FirContainingNamesAwareScope? = error("Should not be called")

            override fun getNestedClassifierScope(
                klass: FirClass,
                useSiteSession: FirSession,
                scopeSession: ScopeSession,
            ): FirContainingNamesAwareScope? = error("Should not be called")
        }

        val session = FirSessionFactoryHelper.createEmptySession()
        val firBuilder = PsiRawFirBuilder(session, scopeProvider)
        val original = firBuilder.buildFirFile(file)

        val designationBuilder = DesignationBuilder(elementToBuild)
        original.accept(designationBuilder)
        val designation = designationBuilder.resultDesignation
        TestCase.assertTrue(designation != null)

        val firElement = RawFirNonLocalDeclarationBuilder.buildWithReplacement(
            session = session,
            scopeProvider = scopeProvider,
            designation!!,
            elementToBuild,
            null
        )

        val firDump = FirRenderer(idRenderer = ConeIdFullRenderer()).renderElementAsString(firElement)
        JUnit5Assertions.assertEqualsToTestDataFileSibling(firDump)
    }

    companion object {
        private const val FUNCTION_DIRECTIVE = "// FUNCTION: "
        private const val PROPERTY_DIRECTIVE = "// PROPERTY: "
    }
}

abstract class AbstractSourcePartialRawFirBuilderTestCase : AbstractPartialRawFirBuilderTestCase() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootPartialRawFirBuilderTestCase : AbstractPartialRawFirBuilderTestCase() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}