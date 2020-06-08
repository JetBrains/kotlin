/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.isSecondaryModule

class SecondaryDeclarationUsageCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (reportOn !is KtReferenceExpression) return

        val resultingDescriptor = resolvedCall.resultingDescriptor
        checkDeclarationUsage(resultingDescriptor, context.moduleDescriptor.platform!!, reportOn, context.trace)
    }
}

class SecondaryDeclarationUsageTypeChecker : ClassifierUsageChecker {
    override fun check(targetDescriptor: ClassifierDescriptor, element: PsiElement, context: ClassifierUsageCheckerContext) {
        if (element !is KtReferenceExpression) return

        checkDeclarationUsage(targetDescriptor, context.moduleDescriptor.platform!!, element, context.trace)
    }
}

private fun checkDeclarationUsage(
    declarationDescriptor: DeclarationDescriptor,
    usagePlatform: TargetPlatform,
    reportOn: KtReferenceExpression,
    trace: BindingTrace,
) {
    val isSecondaryUsage = declarationDescriptor.module.isSecondaryModule
    if (!isSecondaryUsage) return

    val declarationPlatform = declarationDescriptor.module.platform!!

    val explanation = buildExplanationMessage(usagePlatform, declarationPlatform)

    trace.report(
        Errors.SECONDARY_DECLARATION_USAGE.on(reportOn, declarationDescriptor, explanation)
    )
}

private fun buildExplanationMessage(usagePlatform: TargetPlatform, declarationPlatform: TargetPlatform): String {
    return "This reference is declared in module with platform $declarationPlatform, while used in module with platform $usagePlatform"
}
