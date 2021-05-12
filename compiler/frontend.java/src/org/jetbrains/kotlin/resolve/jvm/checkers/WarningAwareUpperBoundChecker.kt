/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.UpperBoundChecker
import org.jetbrains.kotlin.resolve.UpperBoundViolatedReporter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS
import org.jetbrains.kotlin.types.*

// TODO: remove this checker after removing support LV < 1.6
class WarningAwareUpperBoundChecker : UpperBoundChecker() {
    override fun checkBoundsOfExpandedTypeAlias(type: KotlinType, expression: KtExpression, trace: BindingTrace) {
        val typeParameters = type.constructor.parameters

        for ((index, arg) in type.arguments.withIndex()) {
            checkBounds(
                null, arg.type, typeParameters[index], TypeSubstitutor.create(type), trace, expression,
                withOnlyCheckForWarning = true
            )
        }
    }

    override fun checkBounds(
        argumentReference: KtTypeReference?,
        argumentType: KotlinType,
        typeParameterDescriptor: TypeParameterDescriptor,
        substitutor: TypeSubstitutor,
        trace: BindingTrace,
        typeAliasUsageElement: KtElement?
    ) {
        checkBounds(
            argumentReference, argumentType, typeParameterDescriptor, substitutor, trace, typeAliasUsageElement,
            withOnlyCheckForWarning = false
        )
    }

    fun checkBounds(
        argumentReference: KtTypeReference?,
        argumentType: KotlinType,
        typeParameterDescriptor: TypeParameterDescriptor,
        substitutor: TypeSubstitutor,
        trace: BindingTrace,
        typeAliasUsageElement: KtElement? = null,
        withOnlyCheckForWarning: Boolean = false
    ) {
        if (typeParameterDescriptor.upperBounds.isEmpty()) return

        val diagnosticsReporter = UpperBoundViolatedReporter(trace, argumentType, typeParameterDescriptor)
        val diagnosticsReporterForWarnings = UpperBoundViolatedReporter(
            trace, argumentType, typeParameterDescriptor,
            baseDiagnostic = UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS,
            diagnosticForTypeAliases = UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS
        )

        for (bound in typeParameterDescriptor.upperBounds) {
            if (!withOnlyCheckForWarning) {
                val isBaseCheckPassed =
                    checkBound(bound, argumentType, argumentReference, substitutor, typeAliasUsageElement, diagnosticsReporter)

                // The error is already reported, it's unnecessary to do more checks
                if (!isBaseCheckPassed) continue
            }

            val enhancedBound = bound.getEnhancementDeeply() ?: continue
            val argumentTypeEnhancement = argumentType.getEnhancementDeeply() ?: argumentType

            checkBound(
                enhancedBound, argumentTypeEnhancement, argumentReference,
                substitutor, typeAliasUsageElement, diagnosticsReporterForWarnings
            )
        }
    }
}
