/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.isSecondaryModule

class SecondaryDeclarationUsageChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (reportOn !is KtReferenceExpression) return

        val resultingDescriptor = resolvedCall.resultingDescriptor
        val isSecondaryUsage = resultingDescriptor.module.isSecondaryModule
        if (!isSecondaryUsage) return

        val explanation = buildExplanationMessage(resolvedCall, context)

        context.trace.report(
            Errors.SECONDARY_DECLARATION_USAGE.on(reportOn, resultingDescriptor, explanation)
        )
    }

    private fun buildExplanationMessage(resolvedCall: ResolvedCall<*>, context: CallCheckerContext): String {
        val usagePlatform = context.moduleDescriptor.platform!!
        val declarationPlatform = resolvedCall.resultingDescriptor.module.platform!!

        return "This reference is declared in module with platform $declarationPlatform, while used in module with platform $usagePlatform"
    }
}