/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaExtensibleApi
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
 * The test verifies that all extensible endpoints are intentionally extensible.
 *
 * All extensible endpoints have to be annotated with `@KaExtensibleApi` annotation.
 */
class AnalysisApiExtensibilityTest : AbstractAnalysisApiSurfaceCodebaseValidationTest() {
    @Test
    fun testExtensibility() = doTest()

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        psiFile.forEachDescendantOfType<KtClassOrObject> {
            assertExtensibility(file, it)
        }
    }

    private fun assertExtensibility(file: File, classOrObject: KtClassOrObject) {
        if (classOrObject.hasAnnotation(KA_EXTENSIBLE_API) || classOrObject.isInheritanceLimited) return

        val actualText = fileTextWithNewAnnotation(classOrObject, SUBCLASS_OPT_IN_ANNOTATION)
        TestDataAssertions.assertEqualsToFile(
            /* message = */
            """
                The inheritance has to be limited to not guarantee its compatibility by default.
                It can be limited by `sealed` modifier (if applicable) or by `@$SUBCLASS_OPT_IN` annotation.
                If the API is designed to be extensible, add `@$KA_EXTENSIBLE_API` annotation to it.
            """.trimIndent(),
            /* expectedFile = */ file,
            /* actual = */ actualText,
        )
    }

    private val KtClassOrObject.isInheritanceLimited: Boolean
        get() = when {
            // Already requires opt-in on subclassing
            hasAnnotation(SUBCLASS_OPT_IN) -> true

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

    private companion object {
        val KA_EXTENSIBLE_API: String = KaExtensibleApi::class.simpleName!!
        val SUBCLASS_OPT_IN: String = SubclassOptInRequired::class.simpleName!!
        val SUBCLASS_OPT_IN_ANNOTATION = "@$SUBCLASS_OPT_IN(KaImplementationDetail::class)"
    }
}
