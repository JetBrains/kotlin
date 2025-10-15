/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kmp.LexerTests.Companion.initializeLexers
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.PsiTestParser
import org.jetbrains.kotlin.kmp.infra.TestParseNode

class FullParserTestsWithPsi : AbstractParserTests<PsiElement>() {
    companion object {
        init {
            // Make sure the static declarations are initialized before time measurements to get more refined results
            initializeLexers()
            initializeParsers()
        }

        fun initializeParsers() {
            org.jetbrains.kotlin.kdoc.parser.KDocElementTypes.KDOC_SECTION
            org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.CLASS
            org.jetbrains.kotlin.KtNodeTypes.KT_FILE

            org.jetbrains.kotlin.kmp.parser.KDocParseNodes.KDOC_SECTION
            org.jetbrains.kotlin.kmp.parser.KtNodeTypes.KT_FILE
        }
    }

    init {
        // Make sure the static declarations are initialized before time measurements to get more refined results
        PsiTestParser.environment
    }

    override val parseMode: ParseMode = ParseMode.Full

    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestParseNode<out PsiElement> =
        PsiTestParser().parse(fileName, text)

    override val oldRecognizerSuffix: String = " (PSI)"

    override val ignoreFilesWithSyntaxError: Boolean = true
}