/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.coroutines.hasFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.hasBuilderInferenceAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

fun isApplicableCallForBuilderInference(descriptor: CallableDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean {
    if (languageVersionSettings.supportsFeature(LanguageFeature.UnrestrictedBuilderInference)) return true

    if (descriptor.isExtension && !descriptor.hasBuilderInferenceAnnotation()) {
        return descriptor.extensionReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == false
    }

    val returnType = descriptor.returnType ?: return false
    return !returnType.contains { it is StubTypeForBuilderInference }
}

fun isBuilderInferenceCall(parameterDescriptor: ValueParameterDescriptor, argument: ValueArgument): Boolean {
    val parameterHasOptIn = parameterDescriptor.hasBuilderInferenceAnnotation() && parameterDescriptor.hasFunctionOrSuspendFunctionType
    val pureExpression = argument.getArgumentExpression()
    val baseExpression = if (pureExpression is KtLabeledExpression) pureExpression.baseExpression else pureExpression

    return parameterHasOptIn &&
            baseExpression is KtLambdaExpression &&
            parameterDescriptor.type.let { it.isBuiltinFunctionalType && it.getReceiverTypeFromFunctionType() != null }
}
