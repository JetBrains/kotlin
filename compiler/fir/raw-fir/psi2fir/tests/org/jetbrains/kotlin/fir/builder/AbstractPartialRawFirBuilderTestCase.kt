/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractPartialRawFirBuilderTestCase : AbstractRawFirBuilderTestCase() {
    override fun doRawFirTest(filePath: String) {
        val fileText = File(filePath).readText()
        val functionName = InTextDirectivesUtils.findStringWithPrefixes(fileText, FUNCTION_DIRECTIVE)
        val propertyName = InTextDirectivesUtils.findStringWithPrefixes(fileText, PROPERTY_DIRECTIVE)

        when {
            functionName != null -> testFunctionPartialBuilding(filePath, functionName)
            propertyName != null -> testPropertyPartialBuilding(filePath, propertyName)
            else -> fail("No '$FUNCTION_DIRECTIVE' or '$PROPERTY_DIRECTIVE' directives found!")
        }

    }

    private fun testFunctionPartialBuilding(filePath: String, nameToFind: String) {
        testPartialBuilding(
            filePath
        ) { file -> file.findDescendantOfType<KtNamedFunction> { it.name == nameToFind }!! }
    }

    private fun testPropertyPartialBuilding(filePath: String, nameToFind: String) {
        testPartialBuilding(
            filePath
        ) { file -> file.findDescendantOfType<KtProperty> { it.name == nameToFind }!! }
    }

    private class DesignationBuilder(private val elementToBuild: KtDeclaration) : FirVisitorVoid() {
        val designation = mutableListOf<FirDeclaration>()
        var originalDeclaration: FirDeclaration? = null
        var built = false

        override fun visitElement(element: FirElement) {
            if (built) return
            when (element) {
                is FirSimpleFunction, is FirProperty -> {
                    if (element.psi == elementToBuild) {
                        originalDeclaration = element as FirDeclaration
                        built = true
                    } else {
                        element.acceptChildren(this)
                    }
                }
                is FirRegularClass -> {
                    designation.add(element)
                    element.acceptChildren(this)
                    if (!built) {
                        designation.removeLast()
                    }
                }
                else -> {
                    element.acceptChildren(this)
                }
            }
        }
    }

    private fun <T : KtElement> testPartialBuilding(
        filePath: String,
        findPsiElement: (KtFile) -> T
    ) {
        val file = createKtFile(filePath)
        val elementToBuild = findPsiElement(file) as KtDeclaration

        val session = FirSessionFactory.createEmptySession()
        val firBuilder = RawFirBuilder(session, StubFirScopeProvider)
        val original = firBuilder.buildFirFile(file)

        val designationBuilder = DesignationBuilder(elementToBuild)
        original.accept(designationBuilder)
        TestCase.assertTrue(designationBuilder.built)

        val firElement = RawFirFragmentForLazyBodiesBuilder.build(
            session,
            StubFirScopeProvider,
            designationBuilder.designation,
            elementToBuild
        )

        val firDump = firElement.render(FirRenderer.RenderMode.WithFqNames)
        val expectedPath = filePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firDump)
    }

    companion object {
        private const val FUNCTION_DIRECTIVE = "// FUNCTION: "
        private const val PROPERTY_DIRECTIVE = "// PROPERTY: "
    }
}
