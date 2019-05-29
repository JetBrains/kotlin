/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.resolve.calls.inference.OldCapturedTypeCreator
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope
import org.jetbrains.kotlin.types.typesApproximation.OldCaptureTypeApproximator

interface SubstitutingScopeProvider {
    fun createSubstitutingScope(workerScope: MemberScope, givenSubstitutor: TypeSubstitutor): SubstitutingScope

    fun provideCapturedTypeCreator(): CapturedTypeCreator

    fun provideApproximator(): CapturedTypeApproximator

    val isNewInferenceEnabled: Boolean

    companion object {
        val DEFAULT: SubstitutingScopeProvider = object : SubstitutingScopeProvider {
            override fun createSubstitutingScope(workerScope: MemberScope, givenSubstitutor: TypeSubstitutor): SubstitutingScope {
                return SubstitutingScope(workerScope, givenSubstitutor, this)
            }

            override fun provideCapturedTypeCreator(): CapturedTypeCreator {
                return OldCapturedTypeCreator
            }

            override fun provideApproximator(): CapturedTypeApproximator {
                return OldCaptureTypeApproximator()
            }

            override val isNewInferenceEnabled: Boolean get() = false
        }
    }
}

interface CapturedTypeCreator {
    fun createCapturedType(typeProjection: TypeProjection): TypeProjection
}

interface CapturedTypeApproximator {
    fun approximateCapturedTypes(typeProjection: TypeProjection?, approximateContravariant: Boolean): TypeProjection?
}