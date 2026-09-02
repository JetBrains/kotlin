/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtExperimentalApi::class)

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KtPsiFactoryTest : KotlinTestWithEnvironment() {
    @Test
    fun testCreateModifierList() {
        val psiFactory = KtPsiFactory(project)
        KtTokens.MODIFIER_KEYWORDS_ARRAY.forEach {
            val modifier = psiFactory.createModifierList(it)
            Assertions.assertTrue(modifier.hasModifier(it))
        }
    }

    @Test
    fun testEmptyRawStringTemplate() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("")
        Assertions.assertEquals("\"\"\"\"\"\"", template.text)
    }

    @Test
    fun testSingleLineRawStringTemplate() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("Foo Bar")
        Assertions.assertEquals("\"\"\"Foo Bar\"\"\"", template.text)
    }

    @Test
    fun testSingleLineRawStringTemplateWithEntries() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("\$Foo \${Bar}")
        Assertions.assertEquals("\"\"\"\$Foo \${Bar}\"\"\"", template.text)
    }

    @Test
    fun testMultiLineRawStringTemplate() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("Foo\nBar\nBaz")
        Assertions.assertEquals("\"\"\"Foo\nBar\nBaz\"\"\"", template.text)
    }

    @Test
    fun testMultiLineRawStringTemplateWithEntries() {
        val psiFactory = KtPsiFactory(project)
        val template = psiFactory.createRawStringTemplate("\$Foo\n\${Bar}")
        Assertions.assertEquals("\"\"\"\$Foo\n\${Bar}\"\"\"", template.text)
    }

    @Test
    fun testCreateEmptyCompanionBlock() {
        val companionBlock = KtPsiFactory(project).createCompanionBlock()

        Assertions.assertEquals("companion {\n}", companionBlock.text)
        Assertions.assertTrue(companionBlock.declarations.isEmpty())
    }

    @Test
    fun testCreateCompanionBlockFromText() {
        val text = """
            companion {
                val answer = 42
                fun getAnswer() = answer
            }
        """.trimIndent()

        val companionBlock = KtPsiFactory(project).createCompanionBlock(text)

        Assertions.assertEquals(text, companionBlock.text)
        Assertions.assertEquals(2, companionBlock.declarations.size)
        Assertions.assertTrue(companionBlock.declarations[0] is KtProperty)
        Assertions.assertTrue(companionBlock.declarations[1] is KtNamedFunction)
    }

    @Test
    fun testCreateCompanionBlockRejectsInvalidText() {
        val psiFactory = KtPsiFactory(project)
        val invalidTexts = listOf(
            "",
            "companion object {}",
            "companion {}\ncompanion {}",
            "companion {}\nval extra = 0",
            "companion {",
        )

        invalidTexts.forEach { text ->
            Assertions.assertThrows(KotlinExceptionWithAttachments::class.java) {
                psiFactory.createCompanionBlock(text)
            }
        }
    }
}
