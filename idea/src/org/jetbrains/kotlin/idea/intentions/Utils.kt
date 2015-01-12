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

package org.jetbrains.kotlin.idea.intentions

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.codeInsight.ShortenReferences
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.caches.resolve.analyze

fun specifyTypeExplicitly(declaration: JetNamedFunction, typeText: String) {
    specifyTypeExplicitly(declaration, JetPsiFactory(declaration).createType(typeText))
}

fun specifyTypeExplicitly(declaration: JetNamedFunction, type: JetType) {
    if (type.isError()) return
    val typeReference = JetPsiFactory(declaration).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    specifyTypeExplicitly(declaration, typeReference)
    ShortenReferences.process(declaration.getTypeReference()!!)
}

fun specifyTypeExplicitly(declaration: JetNamedFunction, typeReference: JetTypeReference) {
    val anchor = declaration.getValueParameterList() ?: return/*incomplete declaration*/
    declaration.addAfter(typeReference, anchor)
    declaration.addAfter(JetPsiFactory(declaration).createColon(), anchor)
}

fun expressionType(expression: JetExpression): JetType? {
    val bindingContext = expression.analyze()
    return bindingContext.get(BindingContext.EXPRESSION_TYPE, expression)
}

fun functionReturnType(function: JetNamedFunction): JetType? {
    val bindingContext = function.analyze()
    val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function)
    if (descriptor == null) return null
    return (descriptor as FunctionDescriptor).getReturnType()
}

enum class ExpressionKind(val text: String) {
    IF : ExpressionKind("if")
    ELSE : ExpressionKind("else")
    WHILE : ExpressionKind("while")
    DOWHILE : ExpressionKind("do...while")
    FOR : ExpressionKind("for")
}

fun JetExpressionImpl.findBlockInExpression(expressionKind: ExpressionKind?): JetBlockExpression? {
    val bodyNode = when (expressionKind) {
        ExpressionKind.IF -> this.getNode().findChildByType(JetNodeTypes.THEN)
        ExpressionKind.ELSE -> this.getNode().findChildByType(JetNodeTypes.ELSE)
        else -> this.getNode().findChildByType(JetNodeTypes.BODY)
    }
    return bodyNode!!.getPsi()!!.getFirstChild() as? JetBlockExpression
}

fun JetExpressionImpl.getExpressionKind(caretLocation: Int): ExpressionKind? {
    when (this) {
        is JetIfExpression -> {
            if (this.getElse() != null) {
                val elseLocation = this.getNode().findChildByType(JetTokens.ELSE_KEYWORD)!!.getPsi()!!.getTextOffset()
                if (caretLocation >= elseLocation) return ExpressionKind.ELSE
            }
            return ExpressionKind.IF
        }
        is JetWhileExpression -> {
            return ExpressionKind.WHILE
        }
        is JetDoWhileExpression -> {
            return ExpressionKind.DOWHILE
        }
        is JetForExpression -> {
            return ExpressionKind.FOR
        }
        else -> {
            return null
        }
    }
}
