/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.getLeftMostReceiverExpression
import org.jetbrains.kotlin.idea.intentions.replaceFirstReceiver
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlineValHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.getDataFlowValueFactory
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
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
        val deparenthesized = KtPsiUtil.safeDeparenthesize(statement, true)
        if (deparenthesized is KtLambdaExpression) return this
        return deparenthesized
    }
    return innerExpression
}

fun KtExpression?.isTrivialStatementBody(): Boolean = when (this?.unwrapBlockOrParenthesis()) {
    is KtIfExpression, is KtBlockExpression -> false
    else -> true
}

fun KtExpression?.isNullExpression(): Boolean = this?.unwrapBlockOrParenthesis()?.node?.elementType == KtNodeTypes.NULL

fun KtExpression?.isNullExpressionOrEmptyBlock(): Boolean =
    this.isNullExpression() || this is KtBlockExpression && this.statements.isEmpty()

fun KtThrowExpression.throwsNullPointerExceptionWithNoArguments(): Boolean {
    val thrownExpression = this.thrownExpression as? KtCallExpression ?: return false

    val nameExpression = thrownExpression.calleeExpression as? KtNameReferenceExpression ?: return false
    val descriptor = nameExpression.resolveToCall()?.resultingDescriptor
    val declDescriptor = descriptor?.containingDeclaration ?: return false

    val exceptionName = DescriptorUtils.getFqName(declDescriptor).asString()
    return exceptionName in constant {
        setOf(
            "kotlin.KotlinNullPointerException",
            "kotlin.NullPointerException",
            "java.lang.NullPointerException"
        )
    } && thrownExpression.valueArguments.isEmpty()
}

fun KtExpression.evaluatesTo(other: KtExpression): Boolean =
    this.unwrapBlockOrParenthesis().text == other.text

fun KtExpression.anyArgumentEvaluatesTo(argument: KtExpression): Boolean {
    val callExpression = this as? KtCallExpression ?: return false
    val arguments = callExpression.valueArguments.map { it.getArgumentExpression() }
    return arguments.any { it?.evaluatesTo(argument) == true } && arguments.all { it is KtNameReferenceExpression }
}

fun KtExpression.convertToIfNotNullExpression(
    conditionLhs: KtExpression,
    thenClause: KtExpression,
    elseClause: KtExpression?
): KtIfExpression {
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
    val occurrenceInConditional = when (val condition = condition) {
        is KtBinaryExpression -> condition.left
        is KtIsExpression -> condition.leftHandSide
        else -> throw AssertionError("Only binary / is expressions are supported here: ${condition?.text}")
    }!!
    KotlinIntroduceVariableHandler.doRefactoring(
        project,
        editor,
        occurrenceInConditional,
        false,
        listOf(occurrenceInConditional, occurrenceInThenClause), null
    )
}

fun KtNameReferenceExpression.inlineIfDeclaredLocallyAndOnlyUsedOnce(editor: Editor?, withPrompt: Boolean) {
    val declaration = this.mainReference.resolve() as? KtProperty ?: return

    val enclosingElement = KtPsiUtil.getEnclosingElementForLocalDeclaration(declaration)
    val isLocal = enclosingElement != null
    if (!isLocal) return

    val scope = LocalSearchScope(enclosingElement!!)

    val references = ReferencesSearch.search(declaration, scope).findAll()
    if (references.size == 1) {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            ApplicationManager.getApplication().invokeLater {
                val handler = KotlinInlineValHandler(withPrompt)
                if (declaration.isValid && handler.canInlineElement(declaration)) {
                    TransactionGuard.getInstance().submitTransactionAndWait {
                        handler.inlineElement(this.project, editor, declaration)
                    }
                }
            }
        } else {
            KotlinInlineValHandler(withPrompt).inlineElement(this.project, editor, declaration)
        }
    }
}

fun KtSafeQualifiedExpression.inlineReceiverIfApplicable(editor: Editor?, withPrompt: Boolean) {
    (this.receiverExpression as? KtNameReferenceExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnce(editor, withPrompt)
}

fun KtBinaryExpression.inlineLeftSideIfApplicable(editor: Editor?, withPrompt: Boolean) {
    (this.left as? KtNameReferenceExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnce(editor, withPrompt)
}

fun KtPostfixExpression.inlineBaseExpressionIfApplicable(editor: Editor?, withPrompt: Boolean) {
    (this.baseExpression as? KtNameReferenceExpression)?.inlineIfDeclaredLocallyAndOnlyUsedOnce(editor, withPrompt)
}

// I.e. stable val/var/receiver
// We exclude stable complex expressions here, because we don't do smartcasts on them (even though they are stable)
fun KtExpression.isStableSimpleExpression(context: BindingContext = this.analyze()): Boolean {
    val dataFlowValue = this.toDataFlowValue(context)
    return dataFlowValue?.isStable == true &&
            dataFlowValue.kind != DataFlowValue.Kind.STABLE_COMPLEX_EXPRESSION

}

fun KtExpression.isStableVal(context: BindingContext = this.analyze()): Boolean {
    return this.toDataFlowValue(context)?.kind == DataFlowValue.Kind.STABLE_VALUE
}

fun elvisPattern(newLine: Boolean): String = if (newLine) "$0\n?: $1" else "$0 ?: $1"

private fun KtExpression.toDataFlowValue(context: BindingContext): DataFlowValue? {
    val expressionType = this.getType(context) ?: return null
    val dataFlowValueFactory = this.getResolutionFacade().getDataFlowValueFactory()
    return dataFlowValueFactory.createDataFlowValue(this, expressionType, context, findModuleDescriptor())
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
            factory.createExpressionByPattern(
                "$0 as? $1",
                it.leftHandSide,
                it.typeReference!!
            )
        }

        return if (baseClauseEvaluatesToReceiver()) {
            if (condition is KtIsExpression) newReceiver!! else baseClause
        } else {
            when {
                condition is KtIsExpression -> {
                    when {
                        baseClause is KtDotQualifiedExpression -> baseClause.replaceFirstReceiver(
                            factory, newReceiver!!, safeAccess = true
                        )
                        hasImplicitReceiverReplaceableBySafeCall() -> factory.createExpressionByPattern(
                            "$0?.$1",
                            newReceiver!!,
                            baseClause
                        ).insertSafeCalls(
                            factory
                        )
                        baseClause is KtCallExpression -> baseClause.replaceCallWithLet(newReceiver!!, factory)
                        else -> error("Illegal state")
                    }
                }
                hasImplicitReceiverReplaceableBySafeCall() -> factory.createExpressionByPattern(
                    "$0?.$1",
                    receiverExpression,
                    baseClause
                ).insertSafeCalls(factory)
                baseClause is KtCallExpression -> {
                    val callee = baseClause.calleeExpression
                    if (callee != null && baseClause.isCallingInvokeFunction(context)) {
                        factory.createExpressionByPattern("$0?.invoke()", callee)
                    } else {
                        baseClause.replaceCallWithLet(receiverExpression, factory)
                    }
                }
                else -> {
                    var replaced = baseClause.insertSafeCalls(factory)
                    if (replaced is KtQualifiedExpression) {
                        val call = replaced.callExpression
                        val callee = call?.calleeExpression
                        if (callee != null && call.isCallingInvokeFunction(context)) {
                            replaced = factory.createExpressionByPattern("$0?.${callee.text}?.invoke()", replaced.receiverExpression)
                        }
                    }
                    replaced
                }
            }
        }
    }

    internal fun KtExpression.isCallingInvokeFunction(context: BindingContext): Boolean {
        if (this !is KtCallExpression) return false
        val resolvedCall = getResolvedCall(context) ?: resolveToCall() ?: return false
        val descriptor = resolvedCall.resultingDescriptor as? SimpleFunctionDescriptor ?: return false
        return descriptor is FunctionInvokeDescriptor || descriptor.isOperator && descriptor.name.asString() == "invoke"
    }

    internal fun getImplicitReceiver(): ImplicitReceiver? {
        val resolvedCall = baseClause.getResolvedCall(context) ?: return null
        if (resolvedCall.getExplicitReceiverValue() != null) return null
        return resolvedCall.getImplicitReceiverValue()
    }

    internal fun hasImplicitReceiverReplaceableBySafeCall(): Boolean =
        receiverExpression is KtThisExpression && getImplicitReceiver() != null

    private fun KtCallExpression.replaceCallWithLet(
        receiver: KtExpression,
        factory: KtPsiFactory
    ): KtExpression {
        val needExplicitParameter = valueArguments.any { it.getArgumentExpression()?.text == "it" }
        val parameterName = if (needExplicitParameter) {
            val scope = getResolutionScope()
            KotlinNameSuggester.suggestNameByName("it") { scope.findVariable(Name.identifier(it), NoLookupLocation.FROM_IDE) == null }
        } else {
            "it"
        }
        return factory.buildExpression {
            appendExpression(receiver)
            appendFixedText("?.let {")
            if (needExplicitParameter) appendFixedText(" $parameterName ->")
            appendExpression(calleeExpression)
            appendFixedText("(")
            valueArguments.forEachIndexed { index, arg ->
                if (index != 0) appendFixedText(", ")
                val argExpression = arg.getArgumentExpression()
                if (argExpression?.evaluatesTo(receiverExpression) == true)
                    appendFixedText(parameterName)
                else
                    appendExpression(argExpression)
            }
            appendFixedText(") }")
        }
    }
}

internal fun KtIfExpression.buildSelectTransformationData(): IfThenToSelectData? {
    val context = analyze()

    val condition = condition?.unwrapBlockOrParenthesis() as? KtOperationExpression ?: return null
    val thenClause = then?.unwrapBlockOrParenthesis()
    val elseClause = `else`?.unwrapBlockOrParenthesis()
    val receiverExpression = condition.checkedExpression()?.unwrapBlockOrParenthesis() ?: return null

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

internal fun KtExpression?.isClauseTransformableToLetOnly(receiver: KtExpression?) =
    this is KtCallExpression && (resolveToCall()?.getImplicitReceiverValue() == null || receiver !is KtThisExpression)

internal fun KtIfExpression.shouldBeTransformed(): Boolean = when (val condition = condition) {
    is KtBinaryExpression -> {
        val baseClause = (if (condition.operationToken == KtTokens.EQEQ) `else` else then)?.unwrapBlockOrParenthesis()
        !baseClause.isClauseTransformableToLetOnly(condition.checkedExpression())
    }
    else -> false
}

fun KtIfExpression.fromIfKeywordToRightParenthesisTextRangeInThis(): TextRange {
    val rightOffset = rightParenthesis?.endOffset ?: return ifKeyword.textRangeIn(this)
    return TextRange(ifKeyword.startOffset, rightOffset).shiftLeft(startOffset)
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

internal fun KtExpression.isElseIf() = parent.node.elementType == KtNodeTypes.ELSE
