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
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlineValHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.constant

fun KtBinaryExpression.expressionComparedToNull(): KtExpression? {
    val operationToken = this.operationToken
    if (operationToken != KtTokens.EQEQ && operationToken != KtTokens.EXCLEQ) return null

    val right = this.right ?: return null
    val left = this.left ?: return null

    val rightIsNull = right.isNullExpression()
    val leftIsNull = left.isNullExpression()
    if (leftIsNull == rightIsNull) return null
    return if (leftIsNull) right else left
}

fun KtExpression.unwrapBlockOrParenthesis(): KtExpression {
    val innerExpression = KtPsiUtil.safeDeparenthesize(this)
    if (innerExpression is KtBlockExpression) {
        val statement = innerExpression.statements.singleOrNull() ?: return this
        return KtPsiUtil.safeDeparenthesize(statement)
    }
    return innerExpression
}

fun KtExpression?.isNullExpression(): Boolean = this?.unwrapBlockOrParenthesis()?.node?.elementType == KtNodeTypes.NULL

fun KtExpression?.isNullExpressionOrEmptyBlock(): Boolean = this.isNullExpression() || this is KtBlockExpression && this.statements.isEmpty()

fun KtThrowExpression.throwsNullPointerExceptionWithNoArguments(): Boolean {
    val thrownExpression = this.thrownExpression
    if (thrownExpression !is KtCallExpression) return false

    val context = this.analyze(BodyResolveMode.PARTIAL)
    val nameExpression = thrownExpression.calleeExpression as? KtNameReferenceExpression ?: return false
    val descriptor = context[BindingContext.REFERENCE_TARGET, nameExpression]
    val declDescriptor = descriptor?.containingDeclaration ?: return false

    val exceptionName = DescriptorUtils.getFqName(declDescriptor).asString()
    return exceptionName in constant { setOf("kotlin.KotlinNullPointerException", "kotlin.NullPointerException", "java.lang.NullPointerException") }
           && thrownExpression.valueArguments.isEmpty()
}

fun KtExpression.evaluatesTo(other: KtExpression): Boolean {
    return this.unwrapBlockOrParenthesis().text == other.text
}

fun KtExpression.convertToIfNotNullExpression(conditionLhs: KtExpression, thenClause: KtExpression, elseClause: KtExpression?): KtIfExpression {
    val condition = KtPsiFactory(this).createExpressionByPattern("$0 != null", conditionLhs)
    return this.convertToIfStatement(condition, thenClause, elseClause)
}

fun KtExpression.convertToIfNullExpression(conditionLhs: KtExpression, thenClause: KtExpression): KtIfExpression {
    val condition = KtPsiFactory(this).createExpressionByPattern("$0 == null", conditionLhs)
    return this.convertToIfStatement(condition, thenClause)
}

fun KtExpression.convertToIfStatement(condition: KtExpression, thenClause: KtExpression, elseClause: KtExpression? = null): KtIfExpression {
    return replaced(KtPsiFactory(this).createIf(condition, thenClause, elseClause))
}

fun KtIfExpression.introduceValueForCondition(occurrenceInThenClause: KtExpression, editor: Editor?) {
    val project = this.project
    val occurrenceInConditional = (this.condition as KtBinaryExpression).left!!
    KotlinIntroduceVariableHandler.doRefactoring(project,
                                                 editor,
                                                 occurrenceInConditional,
                                                 false,
                                                 listOf(occurrenceInConditional, occurrenceInThenClause), null)
}

fun KtNameReferenceExpression.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor: Editor?) {
    val declaration = this.mainReference.resolve() as? KtProperty ?: return

    val enclosingElement = KtPsiUtil.getEnclosingElementForLocalDeclaration(declaration)
    val isLocal = enclosingElement != null
    if (!isLocal) return

    val scope = LocalSearchScope(enclosingElement!!)

    val references = ReferencesSearch.search(declaration, scope).findAll()
    if (references.size == 1) {
        KotlinInlineValHandler().inlineElement(this.project, editor, declaration)
    }
}

fun KtSafeQualifiedExpression.inlineReceiverIfApplicableWithPrompt(editor: Editor?) {
    (this.receiverExpression as? KtNameReferenceExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor)
}

fun KtBinaryExpression.inlineLeftSideIfApplicableWithPrompt(editor: Editor?) {
    (this.left as? KtNameReferenceExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor)
}

fun KtPostfixExpression.inlineBaseExpressionIfApplicableWithPrompt(editor: Editor?) {
    (this.baseExpression as? KtNameReferenceExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnceWithPrompt(editor)
}

fun KtExpression.isStableVariable(context: BindingContext = this.analyze()): Boolean {
    val descriptor = BindingContextUtils.extractVariableDescriptorFromReference(context, this)
    return descriptor is VariableDescriptor &&
           DataFlowValueFactory.isStableValue(descriptor, DescriptorUtils.getContainingModule(descriptor))
}
