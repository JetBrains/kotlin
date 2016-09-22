/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions.copyConcatenatedStringToClipboard

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import java.awt.datatransfer.StringSelection

class CopyConcatenatedStringToClipboardIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
        KtBinaryExpression::class.java, "Copy concatenation text to clipboard") {
    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val text = ConcatenatedStringGenerator().create(element)
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (element.operationToken != KtTokens.PLUS) return false
        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return false
        return KotlinBuiltIns.isString(resolvedCall.candidateDescriptor.returnType)
    }
}