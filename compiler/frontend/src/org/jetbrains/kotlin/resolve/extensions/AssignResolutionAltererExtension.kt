/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.extensions

import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingComponents
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo


@InternalNonStableExtensionPoints
interface AssignResolutionAltererExtension : AnnotationBasedExtension {
    companion object : ProjectExtensionDescriptor<AssignResolutionAltererExtension>(
        "org.jetbrains.kotlin.assignResolutionAltererExtension",
        AssignResolutionAltererExtension::class.java
    )

    fun needOverloadAssign(expression: KtBinaryExpression, leftType: KotlinType?, bindingContext: BindingContext): Boolean

    fun resolveAssign(
        bindingContext: BindingContext,
        expression: KtBinaryExpression,
        leftOperand: KtExpression,
        left: KtExpression,
        leftInfo: KotlinTypeInfo,
        context: ExpressionTypingContext,
        components: ExpressionTypingComponents,
        scope: LexicalWritableScope
    ): KotlinTypeInfo?
}
