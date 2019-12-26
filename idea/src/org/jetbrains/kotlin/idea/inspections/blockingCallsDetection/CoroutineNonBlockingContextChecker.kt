/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.blockingCallsDetection

import com.intellij.codeInspection.blockingCallsDetection.NonBlockingContextChecker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.debugger.sequence.psi.receiverValue
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.checkers.isRestrictsSuspensionReceiver
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.KotlinType

class CoroutineNonBlockingContextChecker : NonBlockingContextChecker {

    override fun isApplicable(file: PsiFile): Boolean {
        val languageVersionSettings = getLanguageVersionSettings(file)
        return languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
    }

    override fun isContextNonBlockingFor(element: PsiElement): Boolean {
        if (element !is KtCallExpression) return false

        val containingLambda = element.parents
            .firstOrNull { it is KtLambdaExpression && it.analyze().get(BindingContext.LAMBDA_INVOCATIONS, it) == null }
        val containingArgument = containingLambda?.getParentOfType<KtValueArgument>(true, KtCallableDeclaration::class.java)
        if (containingArgument != null) {
            val callExpression = containingArgument.getStrictParentOfType<KtCallExpression>() ?: return false
            val call = callExpression.resolveToCall(BodyResolveMode.PARTIAL) ?: return false

            val isBlockFriendlyDispatcherUsed = call.hasBlockFriendlyDispatcherParameter()
                    || callExpression.isInFunctionWithDefaultDispatcher()
                    || callExpression.isFlowChainElementWithIODispatcher()
            if (isBlockFriendlyDispatcherUsed) return false

            val parameterForArgument = call.getParameterForArgument(containingArgument) ?: return false
            val type = parameterForArgument.returnType ?: return false

            val hasRestrictSuspensionAnnotation = if (type.isBuiltinFunctionalType) {
                type.getReceiverTypeFromFunctionType()?.isRestrictsSuspensionReceiver(getLanguageVersionSettings(element))
            } else null

            return hasRestrictSuspensionAnnotation != true && type.isSuspendFunctionType
        }

        if (containingLambda == null) {
            return element.parentsOfType<KtNamedFunction>()
                .take(2)
                .firstOrNull { function -> function.nameIdentifier != null }
                ?.hasModifier(KtTokens.SUSPEND_KEYWORD) ?: false
        }
        val containingPropertyOrFunction: KtCallableDeclaration? =
            containingLambda.getParentOfTypes(true, KtProperty::class.java, KtNamedFunction::class.java)
        if (containingPropertyOrFunction?.typeReference?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true) return true
        return containingPropertyOrFunction?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true
    }

    private fun getLanguageVersionSettings(psiElement: PsiElement): LanguageVersionSettings =
        psiElement.module?.languageVersionSettings ?: psiElement.project.getLanguageVersionSettings()

    private fun ResolvedCall<*>.getFirstArgument(): KtExpression? =
        valueArgumentsByIndex?.firstOrNull()?.arguments?.firstOrNull()?.getArgumentExpression()

    private fun KotlinType.isCoroutineContext(): Boolean =
        (this.constructor.supertypes + this).any { it.fqName?.asString() == COROUTINE_CONTEXT }


    private fun ResolvedCall<*>.hasBlockFriendlyDispatcherParameter(): Boolean {
        val argumentDescriptor = this.getFirstArgument()?.resolveToCall()?.resultingDescriptor
        return argumentDescriptor.isBlockFriendlyDispatcher()
    }

    private fun KtCallExpression?.isInFunctionWithDefaultDispatcher(): Boolean {
        val propertyDescriptor = (this?.receiverValue() as? ImplicitClassReceiver)?.classDescriptor
            ?.getMemberScope(emptyList())
            ?.getContributedDescriptors(DescriptorKindFilter.VARIABLES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.singleOrNull() ?: return false
        val propertyType = propertyDescriptor.type
        if (!propertyType.isCoroutineContext()) return false

        val initializer = (propertyDescriptor.findPsi() as? KtProperty)?.initializer
        return initializer?.hasBlockFriendlyDispatcher() ?: false
    }

    private fun KtCallExpression?.isFlowChainElementWithIODispatcher(): Boolean {
        tailrec fun KtExpression.findFlowOnCall(): ResolvedCall<out CallableDescriptor>? {
            val dotQualifiedExpression = this.getStrictParentOfType<KtDotQualifiedExpression>() ?: return null
            val candidate = dotQualifiedExpression
                .siblings(withItself = false)
                .asSequence()
                .filterIsInstance<KtCallExpression>()
                .mapNotNull { it.resolveToCall(BodyResolveMode.PARTIAL) }
                .firstOrNull { it.isCalling(FqName(FLOW_ON_FQN)) }
            return candidate ?: dotQualifiedExpression.findFlowOnCall()
        }

        val flowOnCall = this?.findFlowOnCall() ?: return false
        return flowOnCall.hasBlockFriendlyDispatcherParameter()
    }


    private fun KtExpression.hasBlockFriendlyDispatcher(): Boolean {
        class RecursiveExpressionVisitor : PsiRecursiveElementVisitor() {
            var isBlockFriendlyDispatcherFound: Boolean = false

            override fun visitElement(element: PsiElement?) {
                if (element is KtExpression) {
                    val callableDescriptor = element.getCallableDescriptor()
                    if ((callableDescriptor as? DeclarationDescriptor).isBlockFriendlyDispatcher()) {
                        isBlockFriendlyDispatcherFound = true
                        return
                    }
                }
                super.visitElement(element)
            }
        }

        return RecursiveExpressionVisitor().also { this.accept(it) }.isBlockFriendlyDispatcherFound
    }

    private fun DeclarationDescriptor?.isBlockFriendlyDispatcher(): Boolean {
        if (this == null) return false

        val hasBlockingAnnotation = this.annotations.hasAnnotation(FqName(BLOCKING_CONTEXT_ANNOTATION))
        if (hasBlockingAnnotation) return true

        return this.fqNameOrNull()?.asString() == IO_DISPATCHER_FQN
    }

    companion object {
        private const val BLOCKING_CONTEXT_ANNOTATION = "org.jetbrains.annotations.BlockingContext"
        private const val IO_DISPATCHER_FQN = "kotlinx.coroutines.Dispatchers.IO"
        private const val COROUTINE_CONTEXT = "kotlin.coroutines.CoroutineContext"
        private const val FLOW_ON_FQN = "kotlinx.coroutines.flow.flowOn"
    }
}