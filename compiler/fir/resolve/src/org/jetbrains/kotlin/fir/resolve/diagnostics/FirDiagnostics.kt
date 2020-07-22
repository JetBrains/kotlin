/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.CandidateApplicability
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic

class ConeUnresolvedReferenceError(val name: Name? = null) : ConeDiagnostic() {
    override val reason: String get() = "Unresolved reference" + if (name != null) ": ${name.asString()}" else ""
}

class ConeUnresolvedSymbolError(val classId: ClassId) : ConeDiagnostic() {
    override val reason: String get() = "Symbol not found for $classId"
}

class ConeUnresolvedNameError(val name: Name) : ConeDiagnostic() {
    override val reason: String get() = "Unresolved name: $name"
}

class ConeHiddenCandidateError(
    val candidateSymbol: AbstractFirBasedSymbol<*>
) : ConeDiagnostic() {
    override val reason: String get() = "HIDDEN: ${describeSymbol(candidateSymbol)} is invisible"
}

class ConeInapplicableCandidateError(
    val applicability: CandidateApplicability,
    val candidateSymbol: AbstractFirBasedSymbol<*>,
    val diagnostics: List<KotlinCallDiagnostic>
) : ConeDiagnostic() {
    constructor(applicability: CandidateApplicability, candidate: Candidate) : this(
        applicability,
        candidate.symbol,
        candidate.system.diagnostics
    )

    override val reason: String get() = "Inapplicable($applicability): ${describeSymbol(candidateSymbol)}"
}

class ConeAmbiguityError(val name: Name, val applicability: CandidateApplicability, val candidates: Collection<AbstractFirBasedSymbol<*>>) : ConeDiagnostic() {
    override val reason: String get() = "Ambiguity: $name, ${candidates.map { describeSymbol(it) }}"
}

class ConeOperatorAmbiguityError(val candidates: Collection<AbstractFirBasedSymbol<*>>) : ConeDiagnostic() {
    override val reason: String get() = "Operator overload ambiguity. Compatible candidates: ${candidates.map { describeSymbol(it) }}"
}

class ConeVariableExpectedError : ConeDiagnostic() {
    override val reason: String get() = "Variable expected"
}

class ConeTypeMismatchError(val expectedType: ConeKotlinType, val actualType: ConeKotlinType) : ConeDiagnostic() {
    override val reason: String
        get() = "Type mismatch. Expected: $expectedType, Actual: $actualType"
}

class ConeContractDescriptionError(override val reason: String) : ConeDiagnostic()

private fun describeSymbol(symbol: AbstractFirBasedSymbol<*>): String {
    return when (symbol) {
        is FirClassLikeSymbol<*> -> symbol.classId.asString()
        is FirCallableSymbol<*> -> symbol.callableId.toString()
        else -> "$symbol"
    }
}
