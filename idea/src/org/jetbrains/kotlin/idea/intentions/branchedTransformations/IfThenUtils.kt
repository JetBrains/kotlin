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
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.getLeftMostReceiverExpression
import org.jetbrains.kotlin.idea.intentions.replaceFirstReceiver
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlineValHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.refactoring.isMultiLine
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
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
    val innerExpression = KtPsiUtil.safeDeparenthesize(this, true)
    if (innerExpression is KtBlockExpression) {
        val statement = innerExpression.statements.singleOrNull() ?: return this
        return KtPsiUtil.safeDeparenthesize(statement, true)
    }
    return innerExpression
}

fun KtExpression?.isTrivialStatementBody(): Boolean = when (this?.unwrapBlockOrParenthesis()) {
    is KtIfExpression, is KtBlockExpression -> false
    else -> true
}

fun KtExpression?.isNullExpression(): Boolean = this?.unwrapBlockOrParenthesis()?.node?.elementType == KtNodeTypes.NULL

fun KtExpression?.isNullExpressionOrEmptyBlock(): Boolean = this.isNullExpression() || this is KtBlockExpression && this.statements.isEmpty()

fun KtThrowExpression.throwsNullPointerExceptionWithNoArguments(): Boolean {
    val thrownExpression = this.thrownExpression as? KtCallExpression ?: return false

    val context = this.analyze(BodyResolveMode.PARTIAL)
    val nameExpression = thrownExpression.calleeExpression as? KtNameReferenceExpression ?: return false
    val descriptor = context[BindingContext.REFERENCE_TARGET, nameExpression]
    val declDescriptor = descriptor?.containingDeclaration ?: return false

    val exceptionName = DescriptorUtils.getFqName(declDescriptor).asString()
    return exceptionName in constant { setOf("kotlin.KotlinNullPointerException", "kotlin.NullPointerException", "java.lang.NullPointerException") }
           && thrownExpression.valueArguments.isEmpty()
}

fun KtExpression.evaluatesTo(other: KtExpression): Boolean =
        this.unwrapBlockOrParenthesis().text == other.text

fun KtExpression.convertToIfNotNullExpression(conditionLhs: KtExpression, thenClause: KtExpression, elseClause: KtExpression?): KtIfExpression {
    val condition = KtPsiFactory(this).createExpressionByPattern("$0 != null", conditionLhs)
    return this.convertToIfStatement(condition, thenClause, elseClause)
}

fun KtExpression.convertToIfNullExpression(conditionLhs: KtExpression, thenClause: KtExpression): KtIfExpression {
    val condition = KtPsiFactory(this).createExpressionByPattern("$0 == null", conditionLhs)
    return this.convertToIfStatement(condition, thenClause)
}

fun KtExpression.convertToIfStatement(condition: KtExpression, thenClause: KtExpression, elseClause: KtExpression? = null): KtIfExpression =
        replaced(KtPsiFactory(this).createIf(condition, thenClause, elseClause))

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

fun KtExpression.isStable(context: BindingContext = this.analyze()): Boolean {
    if (this is KtConstantExpression || this is KtThisExpression) return true
    val descriptor = BindingContextUtils.extractVariableDescriptorFromReference(context, this)
    return descriptor is VariableDescriptor &&
           DataFlowValueFactory.isStableValue(descriptor, DescriptorUtils.getContainingModule(descriptor))
}

data class IfThenToSelectData(
        val context: BindingContext,
        val condition: KtOperationExpression,
        val receiverExpression: KtExpression,
        val baseClause: KtExpression?,
        val negatedClause: KtExpression?
) {
    internal fun baseClauseEvaluatesToReceiver() =
            baseClause?.evaluatesTo(receiverExpression) == true

    internal fun replacedBaseClause(factory: KtPsiFactory): KtExpression {
        baseClause ?: error("Base clause must be not-null here")
        val newReceiver = (condition as? KtIsExpression)?.let {
            factory.createExpressionByPattern("$0 as? $1",
                                              it.leftHandSide,
                                              it.typeReference!!)
        }
        return if (baseClauseEvaluatesToReceiver()) {
            if (condition is KtIsExpression) newReceiver!! else baseClause
        }
        else {
            when {
                condition is KtIsExpression -> (baseClause as KtDotQualifiedExpression).replaceFirstReceiver(
                        factory, newReceiver!!, safeAccess = true)
                hasImplicitReceiver() -> factory.createExpressionByPattern("this?.$0", baseClause).insertSafeCalls(factory)
                else -> baseClause.insertSafeCalls(factory)
            }
        }
    }

    internal fun hasImplicitReceiver(): Boolean =
            baseClause.getResolvedCall(context)?.getImplicitReceiverValue() != null
}

internal fun KtIfExpression.buildSelectTransformationData(): IfThenToSelectData? {
    val context = analyze()

    val condition = condition as? KtOperationExpression ?: return null
    val thenClause = then?.unwrapBlockOrParenthesis()
    val elseClause = `else`?.unwrapBlockOrParenthesis()
    val receiverExpression = condition.checkedExpression() ?: return null

    val (baseClause, negatedClause) = when (condition) {
        is KtBinaryExpression -> when (condition.operationToken) {
            KtTokens.EQEQ -> elseClause to thenClause
            KtTokens.EXCLEQ -> thenClause to elseClause
            else -> return null
        }
        is KtIsExpression -> {
            val targetType = context[BindingContext.TYPE, condition.typeReference] ?: return null
            if (TypeUtils.isNullableType(targetType)) return null
            // TODO: the following check can be removed after fix of KT-14576
            val originalType = receiverExpression.getType(context) ?: return null
            if (!targetType.isSubtypeOf(originalType)) return null

            when (condition.isNegated) {
                true -> elseClause to thenClause
                false -> thenClause to elseClause
            }
        }
        else -> return null
    }
    return IfThenToSelectData(context, condition, receiverExpression, baseClause, negatedClause)
}

internal fun KtIfExpression.shouldBeTransformed(): Boolean {
    val condition = condition
    return when (condition) {
        is KtBinaryExpression -> true
        is KtIsExpression -> {
            if (!isMultiLine()) true
            else {
                val baseClause = (if (condition.isNegated) `else` else then)?.unwrapBlockOrParenthesis()
                baseClause !is KtDotQualifiedExpression
            }
        }
        else -> false
    }
}

private fun KtExpression.checkedExpression() = when (this) {
    is KtBinaryExpression -> expressionComparedToNull()
    is KtIsExpression -> leftHandSide
    else -> null
}

internal fun KtExpression.hasNullableType(context: BindingContext): Boolean {
    val type = getType(context) ?: return true
    return TypeUtils.isNullableType(type)
}

internal fun KtExpression.hasFirstReceiverOf(receiver: KtExpression): Boolean {
    val actualReceiver = (this as? KtDotQualifiedExpression)?.getLeftMostReceiverExpression() ?: return false
    return actualReceiver.evaluatesTo(receiver)
}

private fun KtExpression.insertSafeCalls(factory: KtPsiFactory): KtExpression {
    if (this !is KtQualifiedExpression) return this
    val replaced = (if (this is KtDotQualifiedExpression) {
        this.replaced(factory.createExpressionByPattern("$0?.$1", receiverExpression, selectorExpression!!))
    } else this) as KtQualifiedExpression
    replaced.receiverExpression.let { it.replace(it.insertSafeCalls(factory)) }
    return replaced
}

// Returns -1 if cannot obtain a document
internal fun KtExpression.lineCount(): Int {
    val file = containingFile?.virtualFile ?: return -1
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return -1
    return document.getLineNumber(textRange.endOffset) - document.getLineNumber(textRange.startOffset) + 1
}

internal fun KtExpression.isOneLiner(): Boolean = lineCount() == 1

internal fun KtExpression.isElseIf() = parent.node.elementType == KtNodeTypes.ELSE
