/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions.branchedTransformations

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.plugin.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.jet.lang.psi.JetSafeQualifiedExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.psi.PsiElement
import org.jetbrains.jet.plugin.refactoring.inline.KotlinInlineValHandler
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.LocalSearchScope
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetThrowExpression
import org.jetbrains.jet.lang.psi.JetPostfixExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.psi.JetElement

val NULL_PTR_EXCEPTION = "NullPointerException"
val NULL_PTR_EXCEPTION_FQ = "java.lang.NullPointerException"
val KOTLIN_NULL_PTR_EXCEPTION = "KotlinNullPointerException"
val KOTLIN_NULL_PTR_EXCEPTION_FQ = "kotlin.KotlinNullPointerException"

fun JetBinaryExpression.comparesNonNullToNull(): Boolean {
    val operationToken = this.getOperationToken()
    val rhs = this.getRight()
    val lhs = this.getLeft()
    if (rhs == null || lhs == null) return false

    val rightIsNull = rhs.isNullExpression()
    val leftIsNull = lhs.isNullExpression()
    return leftIsNull != rightIsNull && (operationToken == JetTokens.EQEQ || operationToken == JetTokens.EXCLEQ)
}

fun JetExpression.extractExpressionIfSingle(): JetExpression? {
    val innerExpression = JetPsiUtil.deparenthesize(this)
    if (innerExpression is JetBlockExpression) {
        return if (innerExpression.getStatements().size() == 1)
                   JetPsiUtil.deparenthesize(innerExpression.getStatements().first as? JetExpression)
               else
                   null
    }

    return innerExpression
}

fun JetExpression.isStatement(): Boolean {
    val context = AnalyzerFacadeWithCache.getContextForElement(this)

    val expectedType = context.get(BindingContext.EXPECTED_EXPRESSION_TYPE, this);
    val isUnit = expectedType != null && KotlinBuiltIns.getInstance().isUnit(expectedType);

    // Some "statements" are actually expressions returned from lambdas, their expected types are non-null
    val isStatement = context.get(BindingContext.STATEMENT, this) == true && expectedType == null;

    return isStatement || isUnit
}

fun JetBinaryExpression.getNonNullExpression(): JetExpression? = when {
    this.getLeft()?.isNullExpression() == false ->
        this.getLeft()
    this.getRight()?.isNullExpression() == false ->
        this.getRight()
    else ->
        null
}

fun JetExpression.isNullExpression(): Boolean = this.extractExpressionIfSingle()?.getText() == "null"

fun JetExpression.isNullExpressionOrEmptyBlock(): Boolean = this.isNullExpression() || this is JetBlockExpression && this.getStatements().empty

fun JetExpression.isThrowExpression(): Boolean = this.extractExpressionIfSingle() is JetThrowExpression

fun JetThrowExpression.throwsNullPointerExceptionWithNoArguments(): Boolean {
    val thrownExpression = this.getThrownExpression()
    if (thrownExpression !is JetCallExpression) return false

    val context = AnalyzerFacadeWithCache.getContextForElement(this)
    val descriptor = context.get(BindingContext.REFERENCE_TARGET, thrownExpression.getCalleeExpression() as JetSimpleNameExpression)
    val declDescriptor = descriptor?.getContainingDeclaration()
    if (declDescriptor == null) return false

    val exceptionName = DescriptorUtils.getFqName(declDescriptor).asString()
    return (exceptionName == NULL_PTR_EXCEPTION_FQ || exceptionName == KOTLIN_NULL_PTR_EXCEPTION_FQ) && thrownExpression.getValueArguments().isEmpty()
}

fun JetExpression.isNotNullExpression(): Boolean {
    val innerExpression = this.extractExpressionIfSingle()
    return innerExpression != null && innerExpression.getText() != "null"
}

fun JetExpression.evaluatesTo(other: JetExpression): Boolean {
    return this.extractExpressionIfSingle()?.getText() == other.getText()
}

fun JetExpression.convertToIfNotNullExpression(conditionLhs: JetExpression, thenClause: JetExpression, elseClause: JetExpression?): JetIfExpression {
    val condition = JetPsiFactory(this).createExpression("${conditionLhs.getText()} != null")
    return this.convertToIfStatement(condition, thenClause, elseClause)
}

fun JetExpression.convertToIfNullExpression(conditionLhs: JetExpression, thenClause: JetExpression): JetIfExpression {
    val condition = JetPsiFactory(this).createExpression("${conditionLhs.getText()} == null")
    return this.convertToIfStatement(condition, thenClause, null)
}

fun JetExpression.convertToIfStatement(condition: JetExpression, thenClause: JetExpression, elseClause: JetExpression?): JetIfExpression {
    val elseBranch = if (elseClause == null) "" else " else ${elseClause.getText()}"
    val conditionalString = "if (${condition.getText()}) ${thenClause.getText()}$elseBranch"

    val st = this.replace(conditionalString) as JetExpression
    return JetPsiUtil.deparenthesize(st) as JetIfExpression
}

fun JetIfExpression.introduceValueForCondition(occurrenceInThenClause: JetExpression, editor: Editor) {
    val project = this.getProject()
    val occurrenceInConditional = (this.getCondition() as JetBinaryExpression).getLeft()!!
    KotlinIntroduceVariableHandler.doRefactoring(project, editor, occurrenceInConditional, listOf(occurrenceInConditional, occurrenceInThenClause))
}

fun JetElement.replace(expressionAsString: String): PsiElement =
        this.replace(JetPsiFactory(this).createExpression(expressionAsString))

fun JetSimpleNameExpression.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor: Editor) {
    val declaration = this.getReference()?.resolve() as JetDeclaration

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
    val context = AnalyzerFacadeWithCache.getContextForElement(this)
    val descriptor = BindingContextUtils.extractVariableDescriptorIfAny(context, this, false)
    return descriptor is VariableDescriptor && DataFlowValueFactory.isStableVariable(descriptor)
}
