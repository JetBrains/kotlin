/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.kmp.infra.NewParserTestNode
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.TestParseNode

abstract class AbstractParserTests<OldParseElement> : AbstractRecognizerTests<
        OldParseElement,
        NewParserTestNode,
        TestParseNode<out OldParseElement>,
        TestParseNode<out NewParserTestNode>
        >(), Disposable {

    init {
        // TODO: Remove it once KT-81457 is fixed
        System.setProperty("ide.enable.implicit.blocking.context", "false")
    }

    protected val disposable = Disposer.newDisposable("Disposable for `${javaClass.simpleName}`")

    abstract val parseMode: ParseMode

    override val recognizerName: String = "parser"

    override val recognizerSyntaxElementName: String = "parse node"

    override fun recognizeNewSyntaxElement(fileName: String, text: String): TestParseNode<out NewParserTestNode> =
        NewTestParser(parseMode).parse(fileName, text)

    override fun dispose() {
        disposeRootInWriteAction(disposable)
    }
}