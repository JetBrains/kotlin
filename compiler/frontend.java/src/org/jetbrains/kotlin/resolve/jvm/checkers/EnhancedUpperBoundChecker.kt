/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.UpperBoundChecker
import org.jetbrains.kotlin.resolve.UpperBoundViolatedReporter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS
import org.jetbrains.kotlin.types.*

// TODO: remove this checker after removing support LV < 1.6
class EnhancedUpperBoundChecker(languageVersionSettings: LanguageVersionSettings) : UpperBoundChecker(languageVersionSettings) {
    val isTypeEnhancementImprovementsEnabled = languageVersionSettings.supportsFeature(LanguageFeature.ImprovementsAroundTypeEnhancement)

    override fun checkBounds(
        argumentReference: KtTypeReference?,
        argumentType: KotlinType,
        typeParameterDescriptor: TypeParameterDescriptor,
        substitutor: TypeSubstitutor,
        trace: BindingTrace,
        typeAliasUsageElement: KtElement?
    ) {
        if (typeParameterDescriptor.upperBounds.isEmpty()) return

        val diagnosticsReporter = UpperBoundViolatedReporter(trace, argumentType, typeParameterDescriptor)
        val diagnosticsReporterForWarnings = UpperBoundViolatedReporter(
            trace, argumentType, typeParameterDescriptor,
            baseDiagnostic = UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS,
            diagnosticForTypeAliases = UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS
        )

        for (bound in typeParameterDescriptor.upperBounds) {
            val isCheckPassed = checkBound(bound, argumentType, argumentReference, substitutor, typeAliasUsageElement, diagnosticsReporter)

            // The error is already reported, it's unnecessary to do more checks
            if (!isCheckPassed) continue

            // If improvements are enabled, then type parameter's upper bounds will already enhanced, and the error will reported inside the first check
            if (isTypeEnhancementImprovementsEnabled) continue

            val enhancedBound = bound.getEnhancementDeeply() ?: continue

            checkBound(enhancedBound, argumentType, argumentReference, substitutor, typeAliasUsageElement, diagnosticsReporterForWarnings)
        }
    }
}
