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

package org.jetbrains.kotlin.idea.replacement

import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

interface CallKindHandler<TCallElement : KtElement> {
    fun elementToReplace(callElement: TCallElement): KtElement

    fun precheckReplacementPattern(pattern: ReplacementExpression): Boolean

    fun wrapGeneratedExpression(expression: KtExpression): TCallElement

    fun unwrapResult(result: TCallElement): TCallElement
}

object CallExpressionHandler : CallKindHandler<KtExpression> {
    override fun elementToReplace(callElement: KtExpression) = callElement.getQualifiedExpressionForSelectorOrThis()

    override fun precheckReplacementPattern(pattern: ReplacementExpression) = true

    override fun wrapGeneratedExpression(expression: KtExpression) = expression

    override fun unwrapResult(result: KtExpression) = result
}

object AnnotationEntryHandler : CallKindHandler<KtAnnotationEntry> {
    override fun elementToReplace(callElement: KtAnnotationEntry) = callElement

    override fun precheckReplacementPattern(pattern: ReplacementExpression): Boolean {
        //TODO
        return true
    }

    //TODO: how to prohibit wrapping replacement expression into anything?

    override fun wrapGeneratedExpression(expression: KtExpression): KtAnnotationEntry {
        return createByPattern("@Dummy($0)", expression) { KtPsiFactory(expression).createAnnotationEntry(it) }
    }

    override fun unwrapResult(result: KtAnnotationEntry): KtAnnotationEntry {
        val text = result.valueArguments.single().getArgumentExpression()!!.text
        return result.replaced(KtPsiFactory(result).createAnnotationEntry("@" + text))
    }
}