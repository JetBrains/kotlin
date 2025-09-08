/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.PsiTestParser
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class FullParserTestsWithPsi : AbstractParserTestsWithPsi() {
    init {
        // Make sure the static declarations are initialized before time measurements to get more refined results
        PsiTestParser.environment
    }

    override val parseMode: ParseMode = ParseMode.Full

    override val expectedDumpOnWindowsNewLine: String = """kotlin.FILE [1:1..2:1)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  ERROR_ELEMENT [1:1..2)
    BAD_CHARACTER [1:1..2)
  WHITE_SPACE [1:2..2:1)"""

    override val ignoreFilesWithSyntaxError: Boolean = true

    @Disabled("Disabled on PSI because the output differs from LightTree and New Parser, and it's expected")
    @Test
    override fun testDifferentParsingOnLazyBlock() {
    }
}