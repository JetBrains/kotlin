/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.PsiTestParser
import org.jetbrains.kotlin.kmp.infra.TestParseNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullParserTestsWithPsi : AbstractParserTests<PsiElement>() {
    private val psiTestParser = PsiTestParser()

    override val parseMode: ParseMode = ParseMode.Full

    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestParseNode<out PsiElement> =
        psiTestParser.parse(fileName, text)

    override val oldRecognizerSuffix: String = " (PSI)"

    override val ignoreFilesWithSyntaxError: Boolean = true

    @AfterAll
    fun cleanup() {
        psiTestParser.dispose()
    }
}