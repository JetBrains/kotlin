/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions.internal

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

@OptIn(InternalNonStableExtensionPoints::class)
class TypeResolutionInterceptor(project: Project) {
    private val extensions = getInstances(project)

    fun interceptFunctionLiteralDescriptor(
        expression: KtLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ) = extensions.fold(descriptor) { it, extension ->
        extension.interceptFunctionLiteralDescriptor(expression, context, it)
    }

    fun interceptType(
        element: KtElement,
        context: ExpressionTypingContext,
        resultType: KotlinType?
    ): KotlinType? {
        // null means that source code has errors and in such scenarios shouldn't be passed into extension point
        if (resultType == null) return null

        return extensions.fold(resultType) { it, extension ->
            extension.interceptType(element, context, it)
        }
    }

    fun isEmpty() = extensions.isEmpty()

    companion object : ProjectExtensionDescriptor<TypeResolutionInterceptorExtension>(
        "org.jetbrains.kotlin.extensions.internal.typeResolutionInterceptorExtension",
        TypeResolutionInterceptorExtension::class.java
    )
}
