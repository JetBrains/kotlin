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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

internal interface CallKindHandler {
    val elementToReplace: JetElement

    fun precheckReplacementPattern(pattern: ReplaceWithAnnotationAnalyzer.ReplacementExpression): Boolean

    fun wrapGeneratedExpression(expression: JetExpression): JetElement

    fun unwrapResult(result: JetElement): JetElement
}

internal class CallExpressionHandler(callElement: JetExpression) : CallKindHandler {
    override val elementToReplace = callElement.getQualifiedExpressionForSelectorOrThis()

    override fun precheckReplacementPattern(pattern: ReplaceWithAnnotationAnalyzer.ReplacementExpression) = true

    override fun wrapGeneratedExpression(expression: JetExpression) = expression

    override fun unwrapResult(result: JetElement) = result
}

internal class AnnotationEntryHandler(annotationEntry: JetAnnotationEntry) : CallKindHandler {
    override val elementToReplace = annotationEntry

    override fun precheckReplacementPattern(pattern: ReplaceWithAnnotationAnalyzer.ReplacementExpression): Boolean {
        //TODO
        return true
    }

    //TODO: how to prohibit wrapping replacement expression into anything?

    override fun wrapGeneratedExpression(expression: JetExpression): JetAnnotationEntry {
        return createByPattern("@Dummy($0)", expression) { JetPsiFactory(expression).createAnnotationEntry(it) }
    }

    override fun unwrapResult(result: JetElement): JetAnnotationEntry {
        result as JetAnnotationEntry
        val text = result.valueArguments.single().getArgumentExpression()!!.text
        return result.replaced(JetPsiFactory(result).createAnnotationEntry("@" + text))
    }
}