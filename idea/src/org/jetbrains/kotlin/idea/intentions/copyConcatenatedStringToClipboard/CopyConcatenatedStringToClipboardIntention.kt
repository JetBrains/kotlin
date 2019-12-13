/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.copyConcatenatedStringToClipboard

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import java.awt.datatransfer.StringSelection

class CopyConcatenatedStringToClipboardIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java, "Copy concatenation text to clipboard"
) {
    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val text = ConcatenatedStringGenerator().create(element)
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (element.operationToken != KtTokens.PLUS) return false
        val resolvedCall = element.resolveToCall() ?: return false
        return KotlinBuiltIns.isString(resolvedCall.candidateDescriptor.returnType)
    }
}