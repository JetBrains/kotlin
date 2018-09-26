/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.blockingCallsDetection

import com.intellij.codeInspection.blockingCallsDetection.NonBlockingContextChecker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.checkers.isRestrictsSuspensionReceiver
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


class CoroutineNonBlockingContextChecker : NonBlockingContextChecker {

    override fun isApplicable(file: PsiFile): Boolean {
        val languageVersionSettings = file.project.getLanguageVersionSettings()
        return languageVersionSettings.supportsFeature(LanguageFeature.Coroutines) &&
                languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_1_3
    }

    override fun isContextNonBlockingFor(element: PsiElement): Boolean {
        if (element !is KtCallExpression)
            return false

        val containingLambda = element.parents
            .firstOrNull { it is KtLambdaExpression && it.analyze().get(BindingContext.LAMBDA_INVOCATIONS, it) == null }
        val containingArgument = PsiTreeUtil.getParentOfType(containingLambda, KtValueArgument::class.java)
        if (containingArgument != null) {
            val callExpression = PsiTreeUtil.getParentOfType(containingArgument, KtCallExpression::class.java) ?: return false
            val call = callExpression.resolveToCall(BodyResolveMode.FULL) ?: return false

            val hasBlockingAnnotation = call.getFirstArgument()?.resolveToCall()
                ?.resultingDescriptor?.annotations?.hasAnnotation(FqName(BLOCKING_CONTEXT_ANNOTATION))
            if (hasBlockingAnnotation == true)
                return false

            val parameterForArgument = call.getParameterForArgument(containingArgument) ?: return false
            val type = parameterForArgument.returnType ?: return false

            val hasRestrictSuspensionAnnotation = if (type.isBuiltinFunctionalType) {
                type.getReceiverTypeFromFunctionType()?.isRestrictsSuspensionReceiver(element.project.getLanguageVersionSettings())
            } else null

            return hasRestrictSuspensionAnnotation != true && type.isSuspendFunctionType
        }

        val callingMethod = PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java) ?: return false
        return callingMethod.hasModifier(KtTokens.SUSPEND_KEYWORD)
    }

    private fun ResolvedCall<*>.getFirstArgument(): KtExpression? =
        valueArgumentsByIndex?.firstOrNull()?.arguments?.firstOrNull()?.getArgumentExpression()

    companion object {
        private const val BLOCKING_CONTEXT_ANNOTATION = "org.jetbrains.annotations.BlockingContext"
    }
}