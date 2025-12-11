/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PsiApiTest {
    @Test
    fun testIsIdentifier() {
        // Null and empty
        assertFalse((null as String?).isIdentifier())
        assertFalse("".isIdentifier())
        assertFalse(" ".isIdentifier())
        assertFalse("\t".isIdentifier())
        assertFalse("\r".isIdentifier())
        assertFalse("\n".isIdentifier())

        // Basic ASCII identifiers
        assertTrue("abc".isIdentifier())
        assertTrue("Abc".isIdentifier())
        assertTrue("_abc".isIdentifier())
        assertTrue("_".isIdentifier())
        assertTrue("abc123".isIdentifier())
        assertTrue("_123".isIdentifier())

        // Invalid - starts with a digit
        assertFalse("123abc".isIdentifier())
        assertFalse("1".isIdentifier())

        // Invalid - contains punctuation
        assertFalse("abc-def".isIdentifier())
        assertFalse("abc def".isIdentifier())
        assertFalse("abc.def".isIdentifier())
        assertFalse("abc+def".isIdentifier())
        assertFalse("abc!".isIdentifier())

        // Hard keywords
        assertFalse("class".isIdentifier())
        assertFalse("fun".isIdentifier())
        assertFalse("val".isIdentifier())
        assertFalse("var".isIdentifier())
        assertFalse("if".isIdentifier())
        assertFalse("while".isIdentifier())
        assertFalse("return".isIdentifier())

        // Soft keywords
        assertTrue("file".isIdentifier())
        assertTrue("enum".isIdentifier())
        assertTrue("field".isIdentifier())
        assertTrue("context".isIdentifier())

        // Non-ASCII: German umlauts (UTF-8 range 128-256)
        assertTrue("ä".isIdentifier())
        assertTrue("ö".isIdentifier())
        assertTrue("ü".isIdentifier())
        assertTrue("Ä".isIdentifier())
        assertTrue("Ö".isIdentifier())
        assertTrue("Ü".isIdentifier())
        assertTrue("ß".isIdentifier())
        assertTrue("Müller".isIdentifier())
        assertTrue("äöü".isIdentifier())
        assertTrue("Größe".isIdentifier())

        // Non-ASCII: French accented characters
        assertTrue("café".isIdentifier())
        assertTrue("naïve".isIdentifier())
        assertTrue("élève".isIdentifier())
        assertTrue("François".isIdentifier())
        assertTrue("résumé".isIdentifier())
        assertTrue("à".isIdentifier())
        assertTrue("è".isIdentifier())
        assertTrue("é".isIdentifier())
        assertTrue("ê".isIdentifier())
        assertTrue("ç".isIdentifier())

        // Non-ASCII: Spanish characters
        assertTrue("niño".isIdentifier())
        assertTrue("año".isIdentifier())
        assertTrue("ñ".isIdentifier())

        // Non-ASCII: Other Latin-1 Supplement characters (128-256)
        assertTrue("æ".isIdentifier())
        assertTrue("Æ".isIdentifier())
        assertTrue("ø".isIdentifier())
        assertTrue("Ø".isIdentifier())
        assertTrue("å".isIdentifier())
        assertTrue("Å".isIdentifier())

        // Non-ASCII: Cyrillic
        assertTrue("Привет".isIdentifier())
        assertTrue("переменная".isIdentifier())

        // Non-ASCII: Greek
        assertTrue("α".isIdentifier())
        assertTrue("β".isIdentifier())
        assertTrue("Δ".isIdentifier())
        assertTrue("λ".isIdentifier())

        // Non-ASCII: Chinese/Japanese/Korean
        assertTrue("変数".isIdentifier())
        assertTrue("変量".isIdentifier())
        assertTrue("변수".isIdentifier())

        // Non-ASCII with digits and underscores
        assertTrue("café123".isIdentifier())
        assertTrue("_Müller".isIdentifier())
        assertTrue("niño_1".isIdentifier())

        // Invalid - backticks in the middle (not as delimiters)
        assertFalse("abc`def".isIdentifier())
        assertFalse("`abc".isIdentifier())
        assertFalse("abc`".isIdentifier())
        assertFalse("a`b`c".isIdentifier())

        // Escaped identifiers (backticks)
        assertTrue("`class`".isIdentifier())
        assertTrue("`fun`".isIdentifier())
        assertTrue("`with spaces`".isIdentifier())
        assertTrue("`special-chars!`".isIdentifier())
        assertTrue("`123`".isIdentifier())
        assertTrue("`café`".isIdentifier())
        assertTrue("`Müller`".isIdentifier())

        // Invalid escaped identifiers - empty
        assertFalse("``".isIdentifier())

        // Invalid escaped identifiers - contains backtick
        assertFalse("`abc`def`".isIdentifier())
        assertFalse("`ab`cd`".isIdentifier())

        // Invalid escaped identifiers - contains newline
        assertFalse("`abc\ndef`".isIdentifier())
        assertFalse("`\n`".isIdentifier())
    }
}