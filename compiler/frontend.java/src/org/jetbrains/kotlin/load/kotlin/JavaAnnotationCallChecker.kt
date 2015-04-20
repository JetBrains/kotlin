/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.typeUtil.isArrayOfJavaLangClass
import org.jetbrains.kotlin.types.typeUtil.isJavaLangClass
import org.jetbrains.kotlin.types.typeUtil.isJavaLangClassOrArray

public class JavaAnnotationCallChecker : CallChecker {
    override fun <F : CallableDescriptor?> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        val resultingDescriptor = resolvedCall.getResultingDescriptor().getOriginal()
        if (resultingDescriptor !is JavaConstructorDescriptor ||
            resultingDescriptor.getContainingDeclaration().getKind() != ClassKind.ANNOTATION_CLASS) return

        reportErrorsOnPositionedArguments(resolvedCall, context)
        reportWarningOnJavaClassUsages(resolvedCall, context)
    }

    private fun reportWarningOnJavaClassUsages(
            resolvedCall: ResolvedCall<*>,
            context: BasicCallResolutionContext
    ) {
        resolvedCall.getValueArguments().filter { it.getKey().getType().isJavaLangClassOrArray() }.forEach {
            reportOnValueArgument(context, it, ErrorsJvm.JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION)
        }
    }

    private fun reportErrorsOnPositionedArguments(
            resolvedCall: ResolvedCall<*>,
            context: BasicCallResolutionContext
    ) {
        resolvedCall.getValueArguments().filter {
            p ->
            p.key.getName() != JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME &&
            p.value is ExpressionValueArgument &&
            !((p.value as ExpressionValueArgument).getValueArgument()?.isNamed() ?: true)
        }.forEach {
            reportOnValueArgument(context, it, ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION)
        }
    }

    private fun reportOnValueArgument(
            context: BasicCallResolutionContext,
            argument: Map.Entry<ValueParameterDescriptor, ResolvedValueArgument>,
            diagnostic: DiagnosticFactory0<JetExpression>
    ) {
        argument.getValue().getArguments().forEach {
            if (it.getArgumentExpression() != null) {
                context.trace.report(
                        diagnostic.on(
                                it.getArgumentExpression()
                        )
                )
            }
        }
    }
}
