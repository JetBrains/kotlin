/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.jvm

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.AbstractConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

class JvmPlatformOverloadsConflictResolver(
    specificityComparator: TypeSpecificityComparator,
    inferenceComponents: InferenceComponents,
    transformerComponents: BodyResolveComponents
) : AbstractConeCallConflictResolver(specificityComparator, inferenceComponents, transformerComponents) {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (!inferenceComponents.session.languageVersionSettings.supportsFeature(LanguageFeature.PreferJavaFieldOverload)) {
            return candidates
        }
        val result = mutableSetOf<Candidate>()
        for (myCandidate in candidates) {
            when (val me = myCandidate.symbol.fir) {
                is FirProperty -> if (!me.isShadowedByFieldCandidate(candidates)) {
                    result += myCandidate
                }
                is FirField -> if (!me.isShadowedByPropertyCandidate(candidates)) {
                    result += myCandidate
                }
                else -> result += myCandidate
            }
        }
        return result
    }

    private fun FirProperty.isShadowedByFieldCandidate(candidates: Set<Candidate>): Boolean {
        val propertyContainingClassLookupTag = unwrapSubstitutionOverrides().symbol.containingClassLookupTag() ?: return false
        for (otherCandidate in candidates) {
            val field = otherCandidate.symbol.fir as? FirField ?: continue
            val fieldContainingClassLookupTag = field.unwrapFakeOverrides().symbol.containingClassLookupTag()
            if (fieldContainingClassLookupTag != null &&
                !propertyContainingClassLookupTag.strictlyDerivedFrom(fieldContainingClassLookupTag)
            ) {
                // NB: FE 1.0 does class equivalence check here ^^^
                // However, in FIR container classes aren't the same for our samples (see fieldPropertyOverloads.kt)
                // E.g. we can have SomeConcreteJavaEnum for field and kotlin.Enum for static property 'name'
                return true
            }
        }
        return false
    }

    private fun FirField.isShadowedByPropertyCandidate(candidates: Set<Candidate>): Boolean {
        val fieldContainingClassLookupTag = unwrapFakeOverrides().symbol.containingClassLookupTag() ?: return false
        for (otherCandidate in candidates) {
            val property = otherCandidate.symbol.fir as? FirProperty ?: continue
            val propertyContainingClassLookupTag = property.unwrapSubstitutionOverrides().symbol.containingClassLookupTag()
            if (propertyContainingClassLookupTag != null &&
                propertyContainingClassLookupTag.strictlyDerivedFrom(fieldContainingClassLookupTag)
            ) {
                // NB: FE 1.0 does class equivalence check here ^^^
                // However, in FIR container classes aren't the same for our samples (see fieldPropertyOverloads.kt)
                // E.g. we can have SomeConcreteJavaEnum for field and kotlin.Enum for static property 'name'
                return true
            }
        }
        return false
    }

    private fun ConeClassLikeLookupTag.strictlyDerivedFrom(other: ConeClassLikeLookupTag): Boolean {
        if (this == other) return false
        val session = inferenceComponents.session
        val thisClass = this.toFirRegularClassSymbol(session)?.fir ?: return false

        return thisClass.isSubclassOf(other, session, isStrict = true)
    }
}
