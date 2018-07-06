/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION
import org.jetbrains.kotlin.diagnostics.Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isArrayOrArrayLiteral
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isParameterOfAnnotation

class AssigningNamedArgumentToVarargChecker : CallChecker {
    companion object {
        private val migrationDiagnosticsForFunction = MigrationDiagnostics(
            ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION,
            ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR
        )

        private val migrationDiagnosticsForAnnotation = MigrationDiagnostics(
            Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION,
            Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR
        )
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        for ((parameterDescriptor, resolvedArgument) in resolvedCall.valueArguments) {
            for (argument in resolvedArgument.arguments) {
                checkAssignmentOfSingleElementToVararg(argument, parameterDescriptor, context.resolutionContext)
            }
        }
    }

    private fun checkAssignmentOfSingleElementToVararg(
        argument: ValueArgument,
        parameterDescriptor: ValueParameterDescriptor,
        context: ResolutionContext<*>
    ) {
        if (!context.languageVersionSettings.supportsFeature(AssigningArraysToVarargsInNamedFormInAnnotations)) return

        if (!argument.isNamed()) return
        if (!parameterDescriptor.isVararg) return

        val argumentExpression = argument.getArgumentExpression() ?: return

        if (isParameterOfAnnotation(parameterDescriptor)) {
            checkAssignmentOfSingleElementInAnnotation(argument, argumentExpression, context)
        } else {
            checkAssignmentOfSingleElementInFunction(argument, argumentExpression, context, parameterDescriptor)
        }
    }

    private fun checkAssignmentOfSingleElementInAnnotation(
        argument: ValueArgument,
        argumentExpression: KtExpression,
        context: ResolutionContext<*>
    ) {
        if (isArrayOrArrayLiteral(argument, context.trace)) {
            if (argument.hasSpread()) {
                // We want to make calls @Foo(value = [A]) and @Foo(value = *[A]) equivalent
                context.trace.report(Errors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION.on(argumentExpression))
            }
        } else {
            reportMigrationDiagnostic(migrationDiagnosticsForAnnotation, context) { diagnostic ->
                context.trace.report(diagnostic.on(argumentExpression))
            }
        }
    }

    private fun checkAssignmentOfSingleElementInFunction(
        argument: ValueArgument,
        argumentExpression: KtExpression,
        context: ResolutionContext<*>,
        parameterDescriptor: ValueParameterDescriptor
    ) {
        if (!argument.hasSpread()) {
            reportMigrationDiagnostic(migrationDiagnosticsForFunction, context) { diagnostic ->
                context.trace.report(diagnostic.on(argumentExpression, parameterDescriptor.type))
            }
        }
    }

    private fun ValueArgument.hasSpread() = getSpreadElement() != null

    private inline fun <T : DiagnosticFactory<*>> reportMigrationDiagnostic(
        migrationDiagnostics: MigrationDiagnostics<T>,
        context: ResolutionContext<*>,
        report: (T) -> Unit
    ) {
        val (warning, error) = migrationDiagnostics
        if (context.languageVersionSettings.supportsFeature(ProhibitAssigningSingleElementsToVarargsInNamedForm)) {
            report(error)
        } else {
            report(warning)
        }
    }
}

private data class MigrationDiagnostics<T : DiagnosticFactory<*>>(val warning: T, val error: T)