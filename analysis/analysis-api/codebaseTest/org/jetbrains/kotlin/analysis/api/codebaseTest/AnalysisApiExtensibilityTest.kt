/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import org.jetbrains.kotlin.AbstractAnalysisApiExtensibilityTest
import org.junit.jupiter.api.Test


class AnalysisApiExtensibilityTest : AbstractAnalysisApiExtensibilityTest() {
    @Test
    fun testExtensibility() = doTest()

    override val sourceDirectories = listOf(
        SourceDirectory.ForValidation(
            sourcePaths = listOf("src/org/jetbrains/kotlin/analysis/api"),
        )
    )
}
