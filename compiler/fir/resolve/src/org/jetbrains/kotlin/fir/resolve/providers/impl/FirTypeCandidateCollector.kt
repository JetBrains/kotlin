/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCandidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

class FirTypeCandidateCollector(
    private val session: FirSession,
    private val useSiteFile: FirFile?,
    private val containingDeclarations: List<FirDeclaration>,
    private val supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    private val resolveDeprecations: Boolean = true,
) {
    private val candidates: MutableSet<TypeCandidate> = mutableSetOf()

    var applicability: CandidateApplicability? = null
        private set

    fun processCandidate(symbol: FirBasedSymbol<*>, substitutor: ConeSubstitutor? = null) {
        var symbolApplicability = CandidateApplicability.RESOLVED
        var diagnostic: ConeVisibilityError? = null

        if (!symbol.isVisible(useSiteFile, containingDeclarations, supertypeSupplier)) {
            val fromCodeFragment = containingDeclarations.getOrNull(1) is FirCodeFragment
            val applicability =
                if (fromCodeFragment) CandidateApplicability.RESOLVED_LOW_PRIORITY else CandidateApplicability.K2_VISIBILITY_ERROR
            symbolApplicability = minOf(applicability, symbolApplicability)
            if (!fromCodeFragment) {
                diagnostic = ConeVisibilityError(symbol)
            }
        }

        if (resolveDeprecations) {
            if (symbol.isDeprecationLevelHidden(session)) {
                symbolApplicability = minOf(CandidateApplicability.HIDDEN, symbolApplicability)
                diagnostic = null
            }
        }

        if (applicability == null || symbolApplicability > applicability!!) {
            applicability = symbolApplicability
            candidates.clear()
        }
        if (symbolApplicability == applicability) {
            candidates.add(TypeCandidate(symbol, substitutor, diagnostic, symbolApplicability))
        }
    }

    private fun FirBasedSymbol<*>?.isVisible(
        useSiteFile: FirFile?,
        containingDeclarations: List<FirDeclaration>,
        supertypeSupplier: SupertypeSupplier,
    ): Boolean {
        val declaration = this?.fir
        return if (useSiteFile != null && declaration is FirMemberDeclaration) {
            session.visibilityChecker.isVisible(
                declaration,
                session,
                useSiteFile,
                containingDeclarations,
                dispatchReceiver = null,
                isCallToPropertySetter = false,
                supertypeSupplier = supertypeSupplier
            )
        } else {
            true
        }
    }

    fun getResult(): TypeResolutionResult {
        filterOutAmbiguousTypealiases(candidates)

        val candidateCount = candidates.size
        return when {
            candidateCount == 1 -> {
                val candidate = candidates.single()
                TypeResolutionResult.Resolved(candidate)
            }
            candidateCount > 1 -> {
                TypeResolutionResult.Ambiguity(candidates.toList())
            }
            else -> TypeResolutionResult.Unresolved
        }
    }

    private fun filterOutAmbiguousTypealiases(candidates: MutableSet<TypeCandidate>) {
        if (candidates.size <= 1) return

        val aliasesToRemove = mutableSetOf<ClassId>()
        val classTypealiasesThatDontCauseAmbiguity = session.platformClassMapper.classTypealiasesThatDontCauseAmbiguity
        for (candidate in candidates) {
            val symbol = candidate.symbol
            if (symbol is FirClassLikeSymbol<*>) {
                classTypealiasesThatDontCauseAmbiguity[symbol.classId]?.let { aliasesToRemove.add(it) }
            }
        }
        if (aliasesToRemove.isNotEmpty()) {
            candidates.removeAll {
                (it.symbol as? FirClassLikeSymbol)?.classId?.let { classId -> aliasesToRemove.contains(classId) } == true
            }
        }
    }

    sealed class TypeResolutionResult {
        class Ambiguity(val typeCandidates: List<TypeCandidate>) : TypeResolutionResult()
        object Unresolved : TypeResolutionResult()
        class Resolved(val typeCandidate: TypeCandidate) : TypeResolutionResult()

        fun resolvedCandidateOrNull(): TypeCandidate? {
            return (this as? Resolved)?.typeCandidate
        }
    }

    class TypeCandidate(
        override val symbol: FirBasedSymbol<*>,
        val substitutor: ConeSubstitutor?,
        // Currently, it's only ConeVisibilityError
        val diagnostic: ConeDiagnostic?,
        override val applicability: CandidateApplicability,
    ) : AbstractCandidate() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TypeCandidate) return false

            if (symbol != other.symbol) return false

            return true
        }

        override fun hashCode(): Int {
            return symbol.hashCode()
        }

        operator fun component1(): FirBasedSymbol<*> = symbol
        operator fun component2(): ConeSubstitutor? = substitutor
    }
}
