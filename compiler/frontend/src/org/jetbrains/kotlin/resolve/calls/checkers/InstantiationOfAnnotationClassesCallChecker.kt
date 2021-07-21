/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

/**
 * Additional checker that prohibits usage of LanguageFeature.InstantiationOfAnnotationClasses on backends
 * that do not support this feature yet
 */
object InstantiationOfAnnotationClassesCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.InstantiationOfAnnotationClasses)) return
        val calledDescriptor = resolvedCall.resultingDescriptor as? ConstructorDescriptor ?: return
        val constructedClass = calledDescriptor.constructedClass
        val expression = resolvedCall.call.callElement as? KtCallExpression ?: return
        if (DescriptorUtils.isAnnotationClass(constructedClass) && !CallExpressionResolver.canInstantiateAnnotationClass(
                expression,
                context.trace
            )
        ) {
            val supported = constructedClass.declaredTypeParameters.isEmpty()
            if (supported) {
                context.trace.report(Errors.ANNOTATION_CLASS_CONSTRUCTOR_CALL.on(expression))
            } else {
                // already reported in CallExpressionResolver.getCallExpressionTypeInfoWithoutFinalTypeCheck
            }
        }
    }
}