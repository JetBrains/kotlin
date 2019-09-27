/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

class TypeResolutionInterceptor(private val project: Project) {
    val extensions = TypeResolutionInterceptorExtension.getInstances(project)
    fun interceptFunctionLiteralDescriptor(
        expression: KtLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ): AnonymousFunctionDescriptor {
        var resultDescriptor = descriptor
        for (extension in extensions) {
            resultDescriptor = extension.interceptFunctionLiteralDescriptor(
                expression,
                context,
                descriptor
            )
        }
        return resultDescriptor
    }

    fun interceptType(
        element: KtElement,
        context: ExpressionTypingContext,
        resultType: KotlinType
    ): KotlinType {
        var type = resultType
        for (extension in extensions) {
            type = extension.interceptType(element, context, type)
        }
        return type
    }

    fun isEmpty() = extensions.isEmpty()
}
