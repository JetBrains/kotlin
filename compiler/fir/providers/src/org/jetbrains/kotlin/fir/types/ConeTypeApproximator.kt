/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class ConeTypeApproximator(inferenceContext: ConeInferenceContext, languageVersionSettings: LanguageVersionSettings) :
    AbstractTypeApproximator(inferenceContext, languageVersionSettings) {
    fun approximateToSuperType(type: ConeKotlinType, conf: TypeApproximatorConfiguration): ConeKotlinType? {
        if (type.fastPathSkipApproximation(conf)) return null
        return super.approximateToSuperType(type, conf) as ConeKotlinType?
    }

    fun approximateToSubType(type: ConeKotlinType, conf: TypeApproximatorConfiguration): ConeKotlinType? {
        if (type.fastPathSkipApproximation(conf)) return null
        return super.approximateToSubType(type, conf) as ConeKotlinType?
    }

    private fun ConeKotlinType.fastPathSkipApproximation(conf: TypeApproximatorConfiguration): Boolean {
        if (this is ConeClassLikeType && this.typeArguments.isEmpty() &&
            this.lookupTag.let { !it.isLocalClass() && !it.isAnonymousClass() }
        ) {
            return true
        }

        // Since K2, we've not been supposed to approximate captured types after full completion.
        // But we potentially might need to approximate ILTs inside the captured types (not sure if it's really a possible situation).
        // For that, there's a tweak at [AbstractTypeApproximator.approximateCapturedType]
        // under `if (!conf.shouldApproximateCapturedType(ctx, capturedType))` marked with TODO: KT-65228.
        //
        // And that might lead to false-positive approximations because of reaching `depth==4` limit when having a self types with many
        // generics.
        // Its super type being approximated to its truncated version with `out Any?` deeply inside, thus we think that something
        // might have changed inside captured type itself, thus returning approximated version.
        //
        // And in such deeply-generic types approximation might run very slowly, so we just make a fast-path here.
        //
        // In general, it should not change semantics: captured types and their deep approximation in most cases work interchangeably.
        // But just in case, we're missing something, it seems to be nice to have a way to enable it since next LV _and_
        // have a way to enable/disable it when necessary.
        //
        // TODO: Anyway, it seems that this all tweaks will be unnecessary once KT-65228 is fixed
        if (!languageVersionSettings.supportsFeature(LanguageFeature.AvoidApproximationOfRecursiveCapturedTypesWithNoReason)) return false

        // If the approximation configuration is designed to approximate something beside ILT/captured types, let it doing that.
        if (conf !is TypeApproximatorConfiguration.AbstractCapturedTypesAndILTApproximation) return false

        return !contains { mightNeedApproximation(it as ConeKotlinType, conf) }
    }

    private fun mightNeedApproximation(
        type: ConeKotlinType,
        conf: TypeApproximatorConfiguration
    ): Boolean = when (type) {
        is ConeIntegerLiteralType -> true
        is ConeCapturedType -> conf.shouldApproximateCapturedType(ctx, type)
        else -> false
    }
}
