/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors.helpers

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiDocumentManager

internal fun InsertionContext.addSymbolAndInvokeCompletion(symbol: String) {
    this.addSymbolToCompletion(symbol)
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

internal fun InsertionContext.addSymbolToCompletion(symbol: String) {
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
    document.insertString(tailOffset, symbol)
    commitDocument()
    editor.caretModel.moveToOffset(tailOffset)
}

internal fun InsertionContext.addTypeArguments(typeArgumentsCount: Int) {
    when {
        typeArgumentsCount == 0 -> {
            return
        }
        typeArgumentsCount < 0 -> {
            error("Count of type arguments should be non-negative, but was $typeArgumentsCount")
        }
        else -> {
            commitDocument()
            addSymbolToCompletion(createStarTypeArgumentsList(typeArgumentsCount))
        }
    }
}