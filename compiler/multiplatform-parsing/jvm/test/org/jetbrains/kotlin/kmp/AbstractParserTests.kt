/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.NewParserTestNode
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.TestParseNode

abstract class AbstractParserTests<OldParseElement> : AbstractRecognizerTests<
        OldParseElement,
        NewParserTestNode,
        TestParseNode<out OldParseElement>,
        TestParseNode<out NewParserTestNode>
>() {
    abstract val parseMode: ParseMode

    override val recognizerName: String = "parser"

    override val recognizerSyntaxElementName: String = "parse node"

    override fun recognizeNewSyntaxElement(fileName: String, text: String): TestParseNode<out NewParserTestNode> =
        NewTestParser(parseMode).parse(fileName, text)
}