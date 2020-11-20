/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
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
            filePath,
            { file -> file.findDescendantOfType<KtNamedFunction> { it.name == nameToFind }!! },
            RawFirBuilder::buildFunctionWithBody
        )
    }

    private fun testPropertyPartialBuilding(filePath: String, nameToFind: String) {
        testPartialBuilding(
            filePath,
            { file -> file.findDescendantOfType<KtProperty> { it.name == nameToFind }!! },
            RawFirBuilder::buildPropertyWithBody
        )
    }

    private fun <T : KtElement> testPartialBuilding(
        filePath: String,
        findPsiElement: (KtFile) -> T,
        buildFirElement: (RawFirBuilder, T) -> FirElement
    ) {
        val file = createKtFile(filePath)
        val elementToBuild = findPsiElement(file)

        val session = FirSessionFactory.createEmptySession()

        val firElement = buildFirElement(RawFirBuilder(session, StubFirScopeProvider), elementToBuild)

        val firDump = firElement.render(FirRenderer.RenderMode.WithFqNames)
        val expectedPath = filePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firDump)
    }

    companion object {
        private const val FUNCTION_DIRECTIVE = "// FUNCTION: "
        private const val PROPERTY_DIRECTIVE = "// PROPERTY: "
    }
}
