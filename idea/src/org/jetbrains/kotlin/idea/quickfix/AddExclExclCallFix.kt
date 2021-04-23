/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.UNSAFE_CALL
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.resolve.getDataFlowValueFactory
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.isValidOperator
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun getAddExclExclCallFix(element: PsiElement?, checkImplicitReceivers: Boolean = false): AddExclExclCallFix? {
    fun KtExpression?.asFix(implicitReceiver: Boolean = false) = this?.let { AddExclExclCallFix(it, implicitReceiver) }

    val psiElement = element ?: return null
    if ((psiElement as? KtExpression).isNullExpression()) {
        return null
    }
    if (psiElement is LeafPsiElement && psiElement.elementType == KtTokens.DOT) {
        return (psiElement.prevSibling as? KtExpression).asFix()
    }
    return when (psiElement) {
        is KtArrayAccessExpression -> psiElement.asFix()
        is KtOperationReferenceExpression -> {
            when (val parent = psiElement.parent) {
                is KtUnaryExpression -> parent.baseExpression.asFix()
                is KtBinaryExpression -> {
                    val receiver = if (KtPsiUtil.isInOrNotInOperation(parent)) parent.right else parent.left
                    receiver.asFix()
                }
                else -> null
            }
        }
        is KtExpression -> {
            val parent = psiElement.parent
            val context = psiElement.analyze()
            if (checkImplicitReceivers && psiElement.getResolvedCall(context)?.getImplicitReceiverValue() is ExtensionReceiver) {
                val expressionToReplace = parent as? KtCallExpression ?: parent as? KtCallableReferenceExpression ?: psiElement
                expressionToReplace.asFix(implicitReceiver = true)
            } else {
                val targetElement = parent.safeAs<KtCallableReferenceExpression>()?.receiverExpression ?: psiElement
                context[BindingContext.EXPRESSION_TYPE_INFO, targetElement]?.let {
                    val type = it.type

                    val dataFlowValueFactory = targetElement.getResolutionFacade().getDataFlowValueFactory()

                    if (type != null) {
                        val nullability = it.dataFlowInfo.getStableNullability(
                            dataFlowValueFactory.createDataFlowValue(targetElement, type, context, targetElement.findModuleDescriptor())
                        )
                        if (!nullability.canBeNonNull()) return null
                    }
                }
                targetElement.asFix()
            }
        }
        else -> null
    }
}

object UnsafeCallExclExclFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val psiElement = diagnostic.psiElement
        if (diagnostic.factory == UNSAFE_CALL && psiElement is KtArrayAccessExpression) {
            psiElement.arrayExpression?.let { return getAddExclExclCallFix(it) }
        }
        return getAddExclExclCallFix(psiElement, checkImplicitReceivers = true)
    }
}

object SmartCastImpossibleExclExclFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        if (diagnostic.factory !== Errors.SMARTCAST_IMPOSSIBLE) return null
        val element = diagnostic.psiElement as? KtExpression ?: return null

        val analyze = element.analyze(BodyResolveMode.PARTIAL)
        val type = analyze.getType(element)
        if (type == null || !TypeUtils.isNullableType(type)) return null

        val diagnosticWithParameters = Errors.SMARTCAST_IMPOSSIBLE.cast(diagnostic)
        val expectedType = diagnosticWithParameters.a
        if (TypeUtils.isNullableType(expectedType)) return null
        val nullableExpectedType = TypeUtils.makeNullable(expectedType)
        if (!type.isSubtypeOf(nullableExpectedType)) return null

        return getAddExclExclCallFix(element)
    }
}

object MissingIteratorExclExclFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val element = diagnostic.psiElement as? KtExpression ?: return null

        val analyze = element.analyze(BodyResolveMode.PARTIAL)
        val type = analyze.getType(element)
        if (type == null || !TypeUtils.isNullableType(type)) return null

        val descriptor = type.constructor.declarationDescriptor

        fun hasIteratorFunction(classifierDescriptor: ClassifierDescriptor?): Boolean {
            if (classifierDescriptor !is ClassDescriptor) return false

            val memberScope = classifierDescriptor.unsubstitutedMemberScope
            val functions = memberScope.getContributedFunctions(OperatorNameConventions.ITERATOR, NoLookupLocation.FROM_IDE)

            return functions.any { it.isValidOperator() }
        }

        when (descriptor) {
            is TypeParameterDescriptor -> {
                if (descriptor.upperBounds.none { hasIteratorFunction(it.constructor.declarationDescriptor) }) return null
            }
            is ClassifierDescriptor -> {
                if (!hasIteratorFunction(descriptor)) return null
            }
            else -> return null
        }

        return getAddExclExclCallFix(element)
    }
}