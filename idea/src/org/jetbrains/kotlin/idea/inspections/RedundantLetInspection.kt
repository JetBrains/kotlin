/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.lineCount
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isNullable

abstract class RedundantLetInspection : AbstractApplicabilityBasedInspection<KtCallExpression>(
    KtCallExpression::class.java
) {
    override fun inspectionText(element: KtCallExpression) = KotlinBundle.message("redundant.let.call.could.be.removed")

    final override fun inspectionHighlightRangeInElement(element: KtCallExpression) = element.calleeExpression?.textRangeIn(element)

    final override val defaultFixText get() = KotlinBundle.message("remove.let.call")

    final override fun isApplicable(element: KtCallExpression): Boolean {
        if (!element.isLetMethodCall()) return false
        val lambdaExpression = element.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return false
        val parameterName = lambdaExpression.getParameterName() ?: return false

        return isApplicable(
            element,
            lambdaExpression.bodyExpression?.children?.singleOrNull() ?: return false,
            lambdaExpression,
            parameterName
        )
    }

    protected abstract fun isApplicable(
        element: KtCallExpression,
        bodyExpression: PsiElement,
        lambdaExpression: KtLambdaExpression,
        parameterName: String
    ): Boolean

    final override fun applyTo(element: KtCallExpression, project: Project, editor: Editor?) {
        val lambdaExpression = element.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return
        when (val bodyExpression = lambdaExpression.bodyExpression?.children?.singleOrNull() ?: return) {
            is KtDotQualifiedExpression -> bodyExpression.applyTo(element)
            is KtBinaryExpression -> bodyExpression.applyTo(element)
            is KtCallExpression -> bodyExpression.applyTo(element, lambdaExpression.functionLiteral, editor)
            is KtSimpleNameExpression -> deleteCall(element)
        }
    }
}

class SimpleRedundantLetInspection : RedundantLetInspection() {
    override fun isApplicable(
        element: KtCallExpression,
        bodyExpression: PsiElement,
        lambdaExpression: KtLambdaExpression,
        parameterName: String
    ): Boolean = when (bodyExpression) {
        is KtDotQualifiedExpression -> bodyExpression.isApplicable(parameterName)
        is KtSimpleNameExpression -> bodyExpression.text == parameterName
        else -> false
    }
}

class ComplexRedundantLetInspection : RedundantLetInspection() {
    override fun isApplicable(
        element: KtCallExpression,
        bodyExpression: PsiElement,
        lambdaExpression: KtLambdaExpression,
        parameterName: String
    ): Boolean = when (bodyExpression) {
        is KtBinaryExpression ->
            element.parent !is KtSafeQualifiedExpression && bodyExpression.isApplicable(parameterName)
        is KtCallExpression ->
            if (element.parent is KtSafeQualifiedExpression) {
                false
            } else {
                val references = lambdaExpression.functionLiteral.valueParameterReferences(bodyExpression)
                val destructuringDeclaration = lambdaExpression.functionLiteral.valueParameters.firstOrNull()?.destructuringDeclaration
                references.isEmpty() || (references.singleOrNull()?.takeIf { expression ->
                    expression.parents.takeWhile { it != lambdaExpression.functionLiteral }.find { it is KtFunction } == null
                } != null && destructuringDeclaration == null)
            }
        else ->
            false
    }

    override fun inspectionHighlightType(element: KtCallExpression): ProblemHighlightType = if (isSingleLine(element))
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    else
        ProblemHighlightType.INFORMATION
}

private fun KtBinaryExpression.applyTo(element: KtCallExpression) {
    val left = left ?: return
    val factory = KtPsiFactory(element.project)
    when (val parent = element.parent) {
        is KtQualifiedExpression -> {
            val receiver = parent.receiverExpression
            val newLeft = when (left) {
                is KtDotQualifiedExpression -> left.replaceFirstReceiver(factory, receiver, parent is KtSafeQualifiedExpression)
                else -> receiver
            }
            val newExpression = factory.createExpressionByPattern("$0 $1 $2", newLeft, operationReference, right!!)
            parent.replace(newExpression)
        }
        else -> {
            val newLeft = when (left) {
                is KtDotQualifiedExpression -> left.deleteFirstReceiver()
                else -> factory.createThisExpression()
            }
            val newExpression = factory.createExpressionByPattern("$0 $1 $2", newLeft, operationReference, right!!)
            element.replace(newExpression)
        }
    }
}

private fun KtDotQualifiedExpression.applyTo(element: KtCallExpression) {
    when (val parent = element.parent) {
        is KtQualifiedExpression -> {
            val factory = KtPsiFactory(element.project)
            val receiver = parent.receiverExpression
            parent.replace(replaceFirstReceiver(factory, receiver, parent is KtSafeQualifiedExpression))
        }
        else -> {
            element.replace(deleteFirstReceiver())
        }
    }
}

private fun deleteCall(element: KtCallExpression) {
    val parent = element.parent as? KtQualifiedExpression
    if (parent != null) {
        val replacement = parent.selectorExpression?.takeIf { it != element } ?: parent.receiverExpression
        parent.replace(replacement)
    } else {
        element.delete()
    }
}

private fun KtCallExpression.applyTo(element: KtCallExpression, functionLiteral: KtFunctionLiteral, editor: Editor?) {
    val parent = element.parent as? KtQualifiedExpression
    val reference = functionLiteral.valueParameterReferences(this).firstOrNull()
    val replaced = if (parent != null) {
        reference?.replace(parent.receiverExpression)
        parent.replaced(this)
    } else {
        reference?.replace(KtPsiFactory(this).createThisExpression())
        element.replaced(this)
    }
    editor?.caretModel?.moveToOffset(replaced.startOffset)
}

private fun KtBinaryExpression.isApplicable(parameterName: String, isTopLevel: Boolean = true): Boolean {
    val left = left ?: return false
    if (isTopLevel) {
        when (left) {
            is KtNameReferenceExpression -> if (left.text != parameterName) return false
            is KtDotQualifiedExpression -> if (!left.isApplicable(parameterName)) return false
            else -> return false
        }
    } else {
        if (!left.isApplicable(parameterName)) return false
    }

    val right = right ?: return false
    return right.isApplicable(parameterName)
}

private fun KtExpression.isApplicable(parameterName: String): Boolean = when (this) {
    is KtNameReferenceExpression -> text != parameterName
    is KtDotQualifiedExpression -> !hasLambdaExpression() && !nameUsed(parameterName)
    is KtBinaryExpression -> isApplicable(parameterName, isTopLevel = false)
    is KtCallExpression -> isApplicable(parameterName)
    is KtConstantExpression -> true
    else -> false
}

private fun KtCallExpression.isApplicable(parameterName: String): Boolean = valueArguments.all {
    val argumentExpression = it.getArgumentExpression() ?: return@all false
    argumentExpression.isApplicable(parameterName)
}

private fun KtDotQualifiedExpression.isApplicable(parameterName: String): Boolean {
    val context by lazy { analyze(BodyResolveMode.PARTIAL) }
    return !hasLambdaExpression() && getLeftMostReceiverExpression().let { receiver ->
        receiver is KtNameReferenceExpression &&
                receiver.getReferencedName() == parameterName &&
                !nameUsed(parameterName, except = receiver)
    } && callExpression?.getResolvedCall(context) !is VariableAsFunctionResolvedCall && !hasNullableReceiverExtensionCall(context)
}

private fun KtDotQualifiedExpression.hasNullableReceiverExtensionCall(context: BindingContext): Boolean {
    val descriptor = selectorExpression?.getResolvedCall(context)?.resultingDescriptor as? CallableMemberDescriptor ?: return false
    if (descriptor.extensionReceiverParameter?.type?.isNullable() == true) return true
    return (KtPsiUtil.deparenthesize(receiverExpression) as? KtDotQualifiedExpression)?.hasNullableReceiverExtensionCall(context) == true
}

private fun KtDotQualifiedExpression.hasLambdaExpression() = selectorExpression?.anyDescendantOfType<KtLambdaExpression>() ?: false

private fun KtCallExpression.isLetMethodCall() = calleeExpression?.text == "let" && isMethodCall("kotlin.let")

private fun KtLambdaExpression.getParameterName(): String? {
    val parameters = valueParameters
    if (parameters.size > 1) return null
    return if (parameters.size == 1) parameters[0].text else "it"
}

private fun KtExpression.nameUsed(name: String, except: KtNameReferenceExpression? = null): Boolean =
    anyDescendantOfType<KtNameReferenceExpression> { it != except && it.getReferencedName() == name }

private fun KtFunctionLiteral.valueParameterReferences(callExpression: KtCallExpression): List<KtNameReferenceExpression> {
    val context = analyze(BodyResolveMode.PARTIAL)
    val parameterDescriptor = context[BindingContext.FUNCTION, this]?.valueParameters?.singleOrNull() ?: return emptyList()
    val variableDescriptorByName = if (parameterDescriptor is ValueParameterDescriptorImpl.WithDestructuringDeclaration)
        parameterDescriptor.destructuringVariables.associateBy { it.name }
    else
        mapOf(parameterDescriptor.name to parameterDescriptor)

    val callee = (callExpression.calleeExpression as? KtNameReferenceExpression)?.let {
        val descriptor = variableDescriptorByName[it.getReferencedNameAsName()]
        if (descriptor != null && it.getReferenceTargets(context).singleOrNull() == descriptor) listOf(it) else null
    } ?: emptyList()
    return callee + callExpression.valueArguments.flatMap { arg ->
        arg.collectDescendantsOfType<KtNameReferenceExpression>().filter {
            val descriptor = variableDescriptorByName[it.getReferencedNameAsName()]
            descriptor != null && it.getResolvedCall(context)?.resultingDescriptor == descriptor
        }
    }
}

private fun isSingleLine(element: KtCallExpression): Boolean {
    val qualifiedExpression = element.getQualifiedExpressionForSelector() ?: return true
    var receiver = qualifiedExpression.receiverExpression
    if (receiver.lineCount() > 1) return false
    var count = 1
    while (true) {
        if (count > 2) return false
        receiver = (receiver as? KtQualifiedExpression)?.receiverExpression ?: break
        count++
    }
    return true
}