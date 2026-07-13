/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.lexer.KtTokens
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
}
