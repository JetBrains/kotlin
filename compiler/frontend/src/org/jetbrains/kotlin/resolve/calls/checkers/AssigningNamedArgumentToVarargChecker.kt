/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isArrayOrArrayLiteral
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isParameterOfAnnotation
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class AssigningNamedArgumentToVarargChecker : CallChecker {
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
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations)) return

        if (!argument.isNamed()) return
        if (!parameterDescriptor.isVararg) return

        val argumentExpression = argument.getArgumentExpression() ?: return

        if (isParameterOfAnnotation(parameterDescriptor)) {
            checkAssignmentOfSingleElementInAnnotation(argument, argumentExpression, context)
        }
        else {
            checkAssignmentOfSingleElementInFunction(argument, argumentExpression, context)
        }
    }

    private fun checkAssignmentOfSingleElementInAnnotation(
            argument: ValueArgument,
            argumentExpression: KtExpression,
            context: ResolutionContext<*>
    ) {
        if (isArrayOrArrayLiteral(argument, context)) {
            if (argument.hasSpread()) {
                context.trace.report(Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM.on(argumentExpression))
            }
        }
        else {
            context.trace.report(Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM.on(argumentExpression))
        }
    }

    private fun checkAssignmentOfSingleElementInFunction(
            argument: ValueArgument,
            argumentExpression: KtExpression,
            context: ResolutionContext<*>
    ) {
        if (!argument.hasSpread()) {
            context.trace.report(Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM.on(argumentExpression))
        }
    }

    private fun ValueArgument.hasSpread() = getSpreadElement() != null
}