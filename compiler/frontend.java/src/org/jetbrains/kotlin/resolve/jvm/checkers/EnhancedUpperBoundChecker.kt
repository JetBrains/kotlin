/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.UpperBoundChecker
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class EnhancedUpperBoundChecker(override val languageVersionSettings: LanguageVersionSettings) : UpperBoundChecker {
    override fun checkBound(
        bound: KotlinType,
        substitutor: TypeSubstitutor,
        trace: BindingTrace,
        jetTypeArgument: KtTypeReference,
        typeArgument: KotlinType
    ): Boolean {
        val isCheckPassed = super.checkBound(bound, substitutor, trace, jetTypeArgument, typeArgument)

        // The error is already reported, it's unnecessary to do more checks
        if (!isCheckPassed) return false

        val enhancedBound = bound.getEnhancement() ?: return false

        val isTypeEnhancementImprovementsEnabled =
            languageVersionSettings.supportsFeature(LanguageFeature.ImprovementsAroundTypeEnhancement)
        val substitutedBound = substitutor.safeSubstitute(enhancedBound, Variance.INVARIANT)
        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(typeArgument, substitutedBound)) {
            if (isTypeEnhancementImprovementsEnabled) {
                trace.report(Errors.UPPER_BOUND_VIOLATED.on(jetTypeArgument, substitutedBound, typeArgument))
            } else {
                trace.report(ErrorsJvm.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS.on(jetTypeArgument, substitutedBound, typeArgument))
            }
            return false
        }
        return true
    }
}
