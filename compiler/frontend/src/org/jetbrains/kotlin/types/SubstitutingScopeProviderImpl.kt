/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.OldCapturedTypeCreator
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope
import org.jetbrains.kotlin.types.typesApproximation.OldCaptureTypeApproximator

class SubstitutingScopeProviderImpl(private val languageVersionSettings: LanguageVersionSettings) : SubstitutingScopeProvider {
    override val isNewInferenceEnabled: Boolean get() = languageVersionSettings.supportsFeature(LanguageFeature.NewInference)

    override fun createSubstitutingScope(workerScope: MemberScope, givenSubstitutor: TypeSubstitutor): SubstitutingScope {
        return SubstitutingScope(workerScope, givenSubstitutor, this)
    }

    override fun provideApproximator(): CapturedTypeApproximator {
        return if (isNewInferenceEnabled) {
            NoCapturedTypeApproximator
        } else {
            OldCaptureTypeApproximator()
        }
    }

    override fun provideCapturedTypeCreator(): CapturedTypeCreator {
        return if (isNewInferenceEnabled) {
            NoCapturedTypeCreator
        } else {
            OldCapturedTypeCreator
        }
    }
}

object NoCapturedTypeCreator : CapturedTypeCreator {
    override fun createCapturedType(typeProjection: TypeProjection): TypeProjection {
        return typeProjection
    }
}

object NoCapturedTypeApproximator : CapturedTypeApproximator {
    override fun approximateCapturedTypes(typeProjection: TypeProjection?, approximateContravariant: Boolean): TypeProjection? {
        return typeProjection
    }
}