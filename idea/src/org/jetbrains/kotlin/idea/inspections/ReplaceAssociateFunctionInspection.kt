/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.INFORMATION
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.getLastLambdaExpression
import org.jetbrains.kotlin.idea.inspections.AssociateFunction.*
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceAssociateFunctionInspection : AbstractKotlinInspection() {
    companion object {
        private val associateFunctionNames = listOf("associate", "associateTo")
        private val associateFqNames = listOf(FqName("kotlin.collections.associate"), FqName("kotlin.sequences.associate"))
        private val associateToFqNames = listOf(FqName("kotlin.collections.associateTo"), FqName("kotlin.sequences.associateTo"))
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = dotQualifiedExpressionVisitor(fun(dotQualifiedExpression) {
        if (dotQualifiedExpression.languageVersionSettings.languageVersion < LanguageVersion.KOTLIN_1_3) return
        val callExpression = dotQualifiedExpression.callExpression ?: return
        val calleeExpression = callExpression.calleeExpression ?: return
        if (calleeExpression.text !in associateFunctionNames) return

        val context = dotQualifiedExpression.analyze(BodyResolveMode.PARTIAL)
        val fqName = callExpression.getResolvedCall(context)?.resultingDescriptor?.fqNameSafe ?: return
        val isAssociate = fqName in associateFqNames
        val isAssociateTo = fqName in associateToFqNames
        if (!isAssociate && !isAssociateTo) return

        val lambda = callExpression.lambda() ?: return
        if (lambda.valueParameters.size > 1) return
        val functionLiteral = lambda.functionLiteral
        if (functionLiteral.anyDescendantOfType<KtReturnExpression> { it.labelQualifier != null }) return
        val lastStatement = functionLiteral.lastStatement() ?: return
        val (keySelector, valueTransform) = lastStatement.pair(context) ?: return
        val lambdaParameter = context[BindingContext.FUNCTION, functionLiteral]?.valueParameters?.singleOrNull() ?: return

        val (associateFunction, highlightType) = when {
            keySelector.isReferenceTo(lambdaParameter, context) -> {
                val receiver = dotQualifiedExpression.receiverExpression.getResolvedCall(context)?.resultingDescriptor?.returnType ?: return
                if (KotlinBuiltIns.isArray(receiver) || KotlinBuiltIns.isPrimitiveArray(receiver)) return
                ASSOCIATE_WITH to GENERIC_ERROR_OR_WARNING
            }
            valueTransform.isReferenceTo(lambdaParameter, context) ->
                ASSOCIATE_BY to GENERIC_ERROR_OR_WARNING
            else -> {
                if (functionLiteral.bodyExpression?.statements?.size != 1) return
                ASSOCIATE_BY_KEY_AND_VALUE to INFORMATION
            }
        }
        holder.registerProblemWithoutOfflineInformation(
            calleeExpression,
            "Replace '${calleeExpression.text}' with '${associateFunction.name(isAssociateTo)}'",
            isOnTheFly,
            highlightType,
            ReplaceAssociateFunctionFix(associateFunction, isAssociateTo)
        )
    })

    private fun KtExpression.isReferenceTo(descriptor: ValueParameterDescriptor, context: BindingContext): Boolean {
        return (this as? KtNameReferenceExpression)?.getResolvedCall(context)?.resultingDescriptor == descriptor
    }
}

private class ReplaceAssociateFunctionFix(private val function: AssociateFunction, private val hasDestination: Boolean) : LocalQuickFix {
    private val functionName = function.name(hasDestination)

    override fun getName() = "Replace with '$functionName'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val dotQualifiedExpression = descriptor.psiElement.getStrictParentOfType<KtDotQualifiedExpression>() ?: return
        val receiverExpression = dotQualifiedExpression.receiverExpression
        val callExpression = dotQualifiedExpression.callExpression ?: return
        val lambda = callExpression.lambda() ?: return
        val lastStatement = lambda.functionLiteral.lastStatement() ?: return
        val (keySelector, valueTransform) = lastStatement.pair() ?: return

        val psiFactory = KtPsiFactory(dotQualifiedExpression)
        if (function == ASSOCIATE_BY_KEY_AND_VALUE) {
            val destination = if (hasDestination) {
                callExpression.valueArguments.firstOrNull()?.getArgumentExpression() ?: return
            } else {
                null
            }
            val newExpression = psiFactory.buildExpression {
                appendExpression(receiverExpression)
                appendFixedText(".")
                appendFixedText(functionName)
                appendFixedText("(")
                if (destination != null) {
                    appendExpression(destination)
                    appendFixedText(",")
                }
                appendLambda(lambda, keySelector)
                appendFixedText(",")
                appendLambda(lambda, valueTransform)
                appendFixedText(")")
            }
            dotQualifiedExpression.replace(newExpression)
        } else {
            lastStatement.replace(if (function == ASSOCIATE_WITH) valueTransform else keySelector)
            val newExpression = psiFactory.buildExpression {
                appendExpression(receiverExpression)
                appendFixedText(".")
                appendFixedText(functionName)
                val valueArgumentList = callExpression.valueArgumentList
                if (valueArgumentList != null) {
                    appendValueArgumentList(valueArgumentList)
                }
                if (callExpression.lambdaArguments.isNotEmpty()) {
                    appendLambda(lambda)
                }
            }
            dotQualifiedExpression.replace(newExpression)
        }
    }

    private fun BuilderByPattern<KtExpression>.appendLambda(lambda: KtLambdaExpression, body: KtExpression? = lambda.bodyExpression) {
        appendFixedText("{")
        lambda.valueParameters.firstOrNull()?.nameAsName?.also {
            appendName(it)
            appendFixedText("->")
        }
        appendExpression(body)
        appendFixedText("}")
    }

    private fun BuilderByPattern<KtExpression>.appendValueArgumentList(valueArgumentList: KtValueArgumentList) {
        appendFixedText("(")
        valueArgumentList.arguments.forEachIndexed { index, argument ->
            if (index > 0) appendFixedText(",")
            appendExpression(argument.getArgumentExpression())
        }
        appendFixedText(")")
    }
}

private enum class AssociateFunction(private val functionName: String) {
    ASSOCIATE_WITH("associateWith"), ASSOCIATE_BY("associateBy"), ASSOCIATE_BY_KEY_AND_VALUE("associateBy");

    fun name(hasDestination: Boolean): String {
        return if (hasDestination) "${functionName}To" else functionName
    }
}

private fun KtCallExpression.lambda(): KtLambdaExpression? {
    return lambdaArguments.singleOrNull()?.getArgumentExpression() as? KtLambdaExpression ?: getLastLambdaExpression()
}

private fun KtFunctionLiteral.lastStatement(): KtExpression? {
    return bodyExpression?.statements?.lastOrNull()
}

private fun KtExpression.pair(context: BindingContext = analyze(BodyResolveMode.PARTIAL)): Pair<KtExpression, KtExpression>? {
    return when (this) {
        is KtBinaryExpression -> {
            if (operationReference.text != "to") return null
            val left = left ?: return null
            val right = right ?: return null
            left to right
        }
        is KtCallExpression -> {
            if (calleeExpression?.text != "Pair") return null
            if (valueArguments.size != 2) return null
            if (getResolvedCall(context)?.resultingDescriptor?.containingDeclaration?.fqNameSafe != FqName("kotlin.Pair")) return null
            val first = valueArguments[0]?.getArgumentExpression() ?: return null
            val second = valueArguments[1]?.getArgumentExpression() ?: return null
            first to second
        }
        else -> return null
    }
}