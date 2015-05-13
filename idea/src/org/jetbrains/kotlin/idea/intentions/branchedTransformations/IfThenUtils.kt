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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlineValHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory

val NULL_PTR_EXCEPTION_FQ = "java.lang.NullPointerException"
val KOTLIN_NULL_PTR_EXCEPTION_FQ = "kotlin.KotlinNullPointerException"

fun JetBinaryExpression.expressionComparedToNull(): JetExpression? {
    val operationToken = this.getOperationToken()
    if (operationToken != JetTokens.EQEQ && operationToken != JetTokens.EXCLEQ) return null

    val right = this.getRight() ?: return null
    val left = this.getLeft() ?: return null

    val rightIsNull = right.isNullExpression()
    val leftIsNull = left.isNullExpression()
    if (leftIsNull == rightIsNull) return null
    return if (leftIsNull) right else left
}

fun JetExpression.unwrapBlockOrParenthesis(): JetExpression {
    val innerExpression = JetPsiUtil.safeDeparenthesize(this)
    if (innerExpression is JetBlockExpression) {
        val statement = innerExpression.getStatements().singleOrNull() as? JetExpression ?: return this
        return JetPsiUtil.safeDeparenthesize(statement)
    }
    return innerExpression
}

fun JetExpression?.isNullExpression(): Boolean = this?.unwrapBlockOrParenthesis()?.getNode()?.getElementType() == JetNodeTypes.NULL

fun JetExpression?.isNullExpressionOrEmptyBlock(): Boolean = this.isNullExpression() || this is JetBlockExpression && this.getStatements().isEmpty()

fun JetThrowExpression.throwsNullPointerExceptionWithNoArguments(): Boolean {
    val thrownExpression = this.getThrownExpression()
    if (thrownExpression !is JetCallExpression) return false

    val context = this.analyze()
    val descriptor = context.get(BindingContext.REFERENCE_TARGET, thrownExpression.getCalleeExpression() as JetSimpleNameExpression)
    val declDescriptor = descriptor?.getContainingDeclaration() ?: return false

    val exceptionName = DescriptorUtils.getFqName(declDescriptor).asString()
    return (exceptionName == NULL_PTR_EXCEPTION_FQ || exceptionName == KOTLIN_NULL_PTR_EXCEPTION_FQ) && thrownExpression.getValueArguments().isEmpty()
}

fun JetExpression.evaluatesTo(other: JetExpression): Boolean {
    return this.unwrapBlockOrParenthesis().getText() == other.getText()
}

fun JetExpression.convertToIfNotNullExpression(conditionLhs: JetExpression, thenClause: JetExpression, elseClause: JetExpression?): JetIfExpression {
    val condition = JetPsiFactory(this).createExpressionByPattern("$0 != null", conditionLhs)
    return this.convertToIfStatement(condition, thenClause, elseClause)
}

fun JetExpression.convertToIfNullExpression(conditionLhs: JetExpression, thenClause: JetExpression): JetIfExpression {
    val condition = JetPsiFactory(this).createExpressionByPattern("$0 == null", conditionLhs)
    return this.convertToIfStatement(condition, thenClause)
}

fun JetExpression.convertToIfStatement(condition: JetExpression, thenClause: JetExpression, elseClause: JetExpression? = null): JetIfExpression {
    return replaced(JetPsiFactory(this).createIf(condition, thenClause, elseClause))
}

fun JetIfExpression.introduceValueForCondition(occurrenceInThenClause: JetExpression, editor: Editor) {
    val project = this.getProject()
    val occurrenceInConditional = (this.getCondition() as JetBinaryExpression).getLeft()!!
    KotlinIntroduceVariableHandler.doRefactoring(project,
                                                 editor,
                                                 occurrenceInConditional,
                                                 listOf(occurrenceInConditional, occurrenceInThenClause),
                                                 null)
}

fun JetSimpleNameExpression.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor: Editor) {
    val declaration = this.getReference()?.resolve() as JetDeclaration

    if (declaration !is JetProperty) return

    val enclosingElement = JetPsiUtil.getEnclosingElementForLocalDeclaration(declaration)
    val isLocal = enclosingElement != null
    if (!isLocal) return

    val scope = LocalSearchScope(enclosingElement!!)

    val references = ReferencesSearch.search(declaration, scope).findAll()
    if (references.size() == 1) {
        KotlinInlineValHandler().inlineElement(this.getProject(), editor, declaration)
    }
}

fun JetSafeQualifiedExpression.inlineReceiverIfApplicableWithPrompt(editor: Editor) {
    (this.getReceiverExpression() as? JetSimpleNameExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor)
}

fun JetBinaryExpression.inlineLeftSideIfApplicableWithPrompt(editor: Editor) {
    (this.getLeft() as? JetSimpleNameExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor)
}

fun JetPostfixExpression.inlineBaseExpressionIfApplicableWithPrompt(editor: Editor) {
    (this.getBaseExpression() as? JetSimpleNameExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor)
}

fun JetExpression.isStableVariable(): Boolean {
    val context = this.analyze()
    val descriptor = BindingContextUtils.extractVariableDescriptorIfAny(context, this, false)
    return descriptor is VariableDescriptor &&
           DataFlowValueFactory.isStableVariable(descriptor, DescriptorUtils.getContainingModule(descriptor))
}
