/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

abstract class AfterAnalysisHighlightingVisitor protected constructor(
    holder: HighlightInfoHolder, protected var bindingContext: BindingContext
) : AbstractHighlightInfoHolderHighlightingVisitor(holder) {

    protected fun attributeKeyForDeclarationFromExtensions(element: PsiElement, descriptor: DeclarationDescriptor): TextAttributesKey? {
        @Suppress("DEPRECATION")
        return Extensions.getExtensions(HighlighterExtension.EP_NAME).firstNotNullOfOrNull { extension ->
            extension.highlightDeclaration(element, descriptor)
        }
    }

    protected fun attributeKeyForCallFromExtensions(
        expression: KtSimpleNameExpression,
        resolvedCall: ResolvedCall<out CallableDescriptor>
    ): TextAttributesKey? {
        @Suppress("DEPRECATION")
        return Extensions.getExtensions(HighlighterExtension.EP_NAME).firstNotNullOfOrNull { extension ->
            extension.highlightCall(expression, resolvedCall)
        }
    }
}
