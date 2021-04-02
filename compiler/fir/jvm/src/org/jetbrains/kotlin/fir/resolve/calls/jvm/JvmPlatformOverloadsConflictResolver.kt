/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.jvm

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.calls.AbstractConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

class JvmPlatformOverloadsConflictResolver(
    specificityComparator: TypeSpecificityComparator,
    inferenceComponents: InferenceComponents
) : AbstractConeCallConflictResolver(specificityComparator, inferenceComponents) {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (!inferenceComponents.session.languageVersionSettings.supportsFeature(LanguageFeature.PreferJavaFieldOverload)) {
            return candidates
        }
        val result = mutableSetOf<Candidate>()
        outerLoop@ for (myCandidate in candidates) {
            val me = myCandidate.symbol.fir
            if (me is FirProperty && me.symbol.containingClass() != null) {
                for (otherCandidate in candidates) {
                    val other = otherCandidate.symbol.fir
                    if (other is FirField && other.symbol.containingClass() != null) {
                        // NB: FE 1.0 does class equivalence check here
                        // However, in FIR container classes aren't the same for our samples (see fieldPropertyOverloads.kt)
                        // E.g. we can have SomeConcreteJavaEnum for field and kotlin.Enum for static property 'name'
                        continue@outerLoop
                    }
                }
            }
            result += myCandidate
        }
        return result
    }
}