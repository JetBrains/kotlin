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

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.load.java.components.JavaAnnotationMapper
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.SimpleCallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JavaAnnotationCallChecker : SimpleCallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor.original
        if (resultingDescriptor !is JavaConstructorDescriptor ||
            resultingDescriptor.containingDeclaration.kind != ClassKind.ANNOTATION_CLASS) return

        reportErrorsOnPositionedArguments(resolvedCall, context)
        reportDeprecatedJavaAnnotation(resolvedCall, context)
    }

    private fun reportDeprecatedJavaAnnotation(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        val annotationEntry = resolvedCall.call.callElement as? KtAnnotationEntry ?: return
        val type = context.trace.get(BindingContext.TYPE, annotationEntry.typeReference) ?: return
        JavaAnnotationMapper.javaToKotlinNameMap[type.constructor.declarationDescriptor?.let { DescriptorUtils.getFqNameSafe(it) }]?.let {
            context.trace.report(ErrorsJvm.DEPRECATED_JAVA_ANNOTATION.on(annotationEntry, it))
        }
    }

    private fun reportErrorsOnPositionedArguments(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        getJavaAnnotationCallValueArgumentsThatShouldBeNamed(resolvedCall).forEach {
            reportOnValueArgument(context, it, ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION)
        }
    }

    private fun reportOnValueArgument(
            context: BasicCallResolutionContext,
            argument: Map.Entry<ValueParameterDescriptor, ResolvedValueArgument>,
            diagnostic: DiagnosticFactory0<KtExpression>
    ) {
        argument.value.arguments.forEach {
            if (it.getArgumentExpression() != null) {
                context.trace.report(
                        diagnostic.on(
                                it.getArgumentExpression()!!
                        )
                )
            }
        }
    }
}
