/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseValidationTest
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The test verifies that the inheritance of the Standalone Analysis API surface is intentionally limited.
 *
 * The rule mirrors `AnalysisApiExtensibilityTest` from the `analysis-api` module, without its `@KaSpi` handling.
 */
class AnalysisApiStandaloneExtensibilityTest : AbstractAnalysisApiCodebaseValidationTest() {
    @Test
    fun testExtensibility() = doTest()

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        psiFile.forEachDescendantOfType<KtClassOrObject> {
            assertExtensibility(file, it)
        }
    }

    private fun assertExtensibility(file: File, classOrObject: KtClassOrObject) {
        if (classOrObject.isInheritanceLimited) return

        val actualText = fileTextWithNewAnnotation(classOrObject, SUBCLASS_OPT_IN_REQUIRED_ANNOTATION)
        TestDataAssertions.assertEqualsToFile(
            /* message = */
            """
                The inheritance has to be limited to not guarantee its compatibility by default.
                It can be limited by `sealed` modifier (if applicable) or by `@$SUBCLASS_OPT_IN_REQUIRED` annotation.
            """.trimIndent(),
            /* expectedFile = */ file,
            /* actual = */ actualText,
        )
    }

    private val KtClassOrObject.isInheritanceLimited: Boolean
        get() = when {
            // Already requires opt-in on subclassing
            hasAnnotation(SUBCLASS_OPT_IN_REQUIRED) -> true

            this is KtClass -> when {
                hasModifier(KtTokens.SEALED_KEYWORD) -> true
                hasModifier(KtTokens.OPEN_KEYWORD) -> false
                hasModifier(KtTokens.ABSTRACT_KEYWORD) -> false
                isInterface() -> false
                else -> true
            }

            this is KtObjectDeclaration -> true

            else -> false
        }

    override val sourceDirectories: List<SourceDirectory.ForValidation> = listOf(
        SourceDirectory.ForValidation(
            sourcePaths = listOf(
                "src/org/jetbrains/kotlin/analysis",
            ),
        )
    )

    private companion object {
        private val SUBCLASS_OPT_IN_REQUIRED: String = SubclassOptInRequired::class.simpleName!!
        private val SUBCLASS_OPT_IN_REQUIRED_ANNOTATION: String = "@$SUBCLASS_OPT_IN_REQUIRED(KaImplementationDetail::class)"
    }
}
