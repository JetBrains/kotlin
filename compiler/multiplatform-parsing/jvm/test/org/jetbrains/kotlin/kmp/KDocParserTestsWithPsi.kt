/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class KDocParserTestsWithPsi : AbstractParserTestsWithPsi() {
    override val parseMode: ParseMode = ParseMode.KDocOnly

    override val expectedDumpOnWindowsNewLine: String = ""

    override val printOldRecognizerTimeInfo: Boolean = false

    @Disabled("No dump in KDocOnly mode")
    @Test
    override fun testBinaryOperationPrecedences() {
    }

    @Disabled("No dump in KDocOnly mode")
    @Test
    override fun testElvis() {
    }

    @Disabled("No dump in KDocOnly mode")
    @Test
    override fun testIsExpressions() {
    }
}