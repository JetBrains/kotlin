/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.jvm

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag

class JvmPlatformOverloadsConflictResolver(private val session: FirSession) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.PreferJavaFieldOverload)) {
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
        val thisClass = this.toSymbol(session)?.fir as? FirClass ?: return false

        return thisClass.isSubclassOf(other, session, isStrict = true)
    }
}
