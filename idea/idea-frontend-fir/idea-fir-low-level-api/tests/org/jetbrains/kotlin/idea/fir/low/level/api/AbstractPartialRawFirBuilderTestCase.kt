/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import junit.framework.TestCase
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.RawFirNonLocalDeclarationBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
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
        private val path = mutableListOf<FirDeclaration>()
        var resultDesignation: FirDeclarationDesignation? = null
            private set

        override fun visitElement(element: FirElement) {
            if (resultDesignation != null) return
            when (element) {
                is FirSimpleFunction, is FirProperty -> {
                    if (element.psi == elementToBuild) {
                        val originalDeclaration = element as FirDeclaration
                        resultDesignation = FirDeclarationDesignation(path, originalDeclaration)
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
            override fun getUseSiteMemberScope(klass: FirClass, useSiteSession: FirSession, scopeSession: ScopeSession): FirTypeScope =
                error("Should not be called")

            override fun getStaticMemberScopeForCallables(
                klass: FirClass,
                useSiteSession: FirSession,
                scopeSession: ScopeSession
            ): FirScope =
                error("Should not be called")

            override fun getNestedClassifierScope(klass: FirClass, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope =
                error("Should not be called")
        }

        val session = FirSessionFactory.createEmptySession()
        val firBuilder = RawFirBuilder(session, scopeProvider)
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

        val firDump = firElement.render(FirRenderer.RenderMode.WithFqNames)
        KotlinTestUtils.assertEqualsToFile(testDataFileSibling(".txt"), firDump)
    }

    companion object {
        private const val FUNCTION_DIRECTIVE = "// FUNCTION: "
        private const val PROPERTY_DIRECTIVE = "// PROPERTY: "
    }
}
