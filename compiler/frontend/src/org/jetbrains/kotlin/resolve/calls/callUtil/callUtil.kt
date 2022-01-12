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
import org.jetbrains.kotlin.utils.IDEAPlatforms
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

@IDEAPluginsCompatibilityAPI(
    IDEAPlatforms._213, // I'm not sure about 212 and 211 AS -- didn't check them
    message = "Use org.jetbrains.kotlin.resolve.calls.util.getResolvedCall instead.",
    plugins = "Android in IDEA"
)
fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? = getResolvedCall(context)

@IDEAPluginsCompatibilityAPI(
    IDEAPlatforms._213, // I'm not sure about 212 and 211 AS -- didn't check them
    message = "Use org.jetbrains.kotlin.resolve.calls.util.getResolvedCall instead.",
    plugins = "Android in IDEA"
)
fun KtElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? = getResolvedCall(context)


// TODO: find what IDEA's used it
@IDEAPluginsCompatibilityAPI(
    message = "Use org.jetbrains.kotlin.resolve.calls.util.getType instead."
)
fun KtExpression.getType(context: BindingContext): KotlinType? = getType(context)

@IDEAPluginsCompatibilityAPI(
    IDEAPlatforms._213, // I'm not sure about 212 and 211 AS -- didn't check them
    message = "Use org.jetbrains.kotlin.resolve.calls.util.getCall instead.",
    plugins = "Android in IDEA"
)
fun KtElement.getCall(context: BindingContext): Call? = getCall(context)

// TODO: find what IDEA's used it
@IDEAPluginsCompatibilityAPI(
    message = "Use org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny instead."
)
fun KtElement?.getCalleeExpressionIfAny(): KtExpression? = getCalleeExpressionIfAny()

// TODO: find what IDEA's used it
@IDEAPluginsCompatibilityAPI(
    message = "Use org.jetbrains.kotlin.resolve.calls.util.createLookupLocation instead."
)
fun Call.createLookupLocation(): KotlinLookupLocation = createLookupLocation()

// TODO: find what IDEA's used it
@IDEAPluginsCompatibilityAPI(
    message = "Use org.jetbrains.kotlin.resolve.calls.util.createLookupLocation instead."
)
fun KtExpression.createLookupLocation(): KotlinLookupLocation? = createLookupLocation()

@IDEAPluginsCompatibilityAPI(
    IDEAPlatforms._213, // I'm not sure about 212 and 211 AS -- didn't check them
    message = "Use org.jetbrains.kotlin.resolve.calls.util.getValueArgumentForExpression instead.",
    plugins = "Android in IDEA"
)
fun Call.getValueArgumentForExpression(expression: KtExpression): ValueArgument? = getValueArgumentForExpression(expression)
