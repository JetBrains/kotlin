/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.callUtil

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.util.createLookupLocation
import org.jetbrains.kotlin.resolve.calls.util.getValueArgumentForExpression
import org.jetbrains.kotlin.types.KotlinType

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.getResolvedCall instead.",
    ReplaceWith("getResolvedCall", "org.jetbrains.kotlin.resolve.calls.util.getResolvedCall"),
    level = DeprecationLevel.ERROR
)
fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? = getResolvedCall(context)

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.getResolvedCall instead.",
    ReplaceWith("getResolvedCall", "org.jetbrains.kotlin.resolve.calls.util.getResolvedCall"),
    level = DeprecationLevel.ERROR
)
fun KtElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? = getResolvedCall(context)

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.getType instead.",
    ReplaceWith("getType", "org.jetbrains.kotlin.resolve.calls.util.getType"),
    level = DeprecationLevel.ERROR
)
fun KtExpression.getType(context: BindingContext): KotlinType? = getType(context)

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.getCall instead.",
    ReplaceWith("getType", "org.jetbrains.kotlin.resolve.calls.util.getCall"),
    level = DeprecationLevel.ERROR
)
fun KtElement.getCall(context: BindingContext): Call? = getCall(context)

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny instead.",
    ReplaceWith("getCalleeExpressionIfAny", "org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny"),
    level = DeprecationLevel.ERROR
)
fun KtElement?.getCalleeExpressionIfAny(): KtExpression? = getCalleeExpressionIfAny()

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.createLookupLocation instead.",
    ReplaceWith("createLookupLocation", "org.jetbrains.kotlin.resolve.calls.util.createLookupLocation"),
    level = DeprecationLevel.ERROR
)
fun Call.createLookupLocation(): KotlinLookupLocation = createLookupLocation()

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.createLookupLocation instead.",
    ReplaceWith("createLookupLocation", "org.jetbrains.kotlin.resolve.calls.util.createLookupLocation"),
    level = DeprecationLevel.ERROR
)
fun KtExpression.createLookupLocation(): KotlinLookupLocation? = createLookupLocation()

@Deprecated(
    "Use org.jetbrains.kotlin.resolve.calls.util.getValueArgumentForExpression instead.",
    ReplaceWith("getValueArgumentForExpression", "org.jetbrains.kotlin.resolve.calls.util.getValueArgumentForExpression"),
    level = DeprecationLevel.ERROR
)
fun Call.getValueArgumentForExpression(expression: KtExpression): ValueArgument? = getValueArgumentForExpression(expression)
