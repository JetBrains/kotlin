/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.effectiveVisibility
import org.jetbrains.kotlin.descriptors.impl.LocalVariableAccessorDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil

object LocalDelegatedPropertiesInInlineFunctionsChecker : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is LocalVariableDescriptor) return
        val inlineFunctionDescriptor = generateSequence(descriptor as DeclarationDescriptor) { descriptor.containingDeclaration }
            .firstOrNull { InlineUtil.isInline(it) } as? FunctionDescriptor ?: return
        if (!inlineFunctionDescriptor.effectiveVisibility(checkPublishedApi = true).publicApi) return
        val delegateExpression =
            context.trace.bindingContext.get(BindingContext.DELEGATED_PROPERTY_CALL, descriptor.getter)?.callElement ?: return
        checkAndReportIfCallsNonPublishedApi(descriptor.getter, delegateExpression, inlineFunctionDescriptor, context)
        checkAndReportIfCallsNonPublishedApi(descriptor.setter, delegateExpression, inlineFunctionDescriptor, context)
    }

    private fun checkAndReportIfCallsNonPublishedApi(
        accessor: LocalVariableAccessorDescriptor?,
        delegatedExpression: KtElement,
        inlineFunctionDescriptor: FunctionDescriptor,
        context: DeclarationCheckerContext,
    ) {
        val reportError =
            context.languageVersionSettings.supportsFeature(
                LanguageFeature.ForbidLocalDelegatedPropertiesWithPrivateAccessorsInPublicInlineFunctions
            )
        val delegatedDescriptor =
            context.trace.bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor)?.resultingDescriptor ?: return
        if (!delegatedDescriptor.effectiveVisibility(checkPublishedApi = true).publicApi) {
            if (reportError) {
                context.trace.report(
                    Errors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE.on(
                        delegatedExpression,
                        delegatedDescriptor,
                        inlineFunctionDescriptor
                    )
                )
            } else {
                context.trace.report(Errors.DEPRECATED_IMPLICIT_NON_PUBLIC_API_ACCESS.on(delegatedExpression))
            }
        }
    }
}