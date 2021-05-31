/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors.helpers

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.openapi.application.ApplicationManager

internal fun InsertionContext.addSymbolAndInvokeCompletion(symbol: String) {
    addSymbolToCompletion(this, symbol)
    invokeCompletion(this)
}

private fun invokeCompletion(context: InsertionContext) {
    ApplicationManager.getApplication().invokeLater {
        if (!context.editor.isDisposed) {
            CodeCompletionHandlerBase(CompletionType.BASIC, true, false, true)
                .invokeCompletion(context.project, context.editor)
        }
    }
}

private fun addSymbolToCompletion(context: InsertionContext, symbol: String) {
    context.document.insertString(context.tailOffset, symbol)
    context.commitDocument()
    context.editor.caretModel.moveToOffset(context.tailOffset)
}