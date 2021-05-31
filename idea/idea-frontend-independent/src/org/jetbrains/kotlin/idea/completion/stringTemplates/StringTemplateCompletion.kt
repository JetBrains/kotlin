/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.stringTemplates

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionUtilCore
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset

object StringTemplateCompletion {
    fun correctParametersForInStringTemplateCompletion(parameters: CompletionParameters): CompletionParameters? {
        val position = parameters.position
        if (position.node.elementType == KtTokens.LONG_TEMPLATE_ENTRY_START) {
            val expression = (position.parent as? KtBlockStringTemplateEntry)?.expression
            if (expression is KtDotQualifiedExpression) {
                val correctedPosition = (expression.selectorExpression as? KtNameReferenceExpression)?.firstChild
                if (correctedPosition != null) {
                    // Workaround for KT-16848
                    // ex:
                    // expression: some.IntellijIdeaRulezzz
                    // correctedOffset: ^
                    // expression: some.funcIntellijIdeaRulezzz
                    // correctedOffset      ^
                    val correctedOffset = correctedPosition.endOffset - CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.length
                    return parameters.withPosition(correctedPosition, correctedOffset)
                }
            }
        }
        return null
    }

}