/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics

import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

sealed class ConeUnresolvedError : ConeDiagnostic() {
    abstract val qualifier: String?
}

class ConeUnresolvedReferenceError(val name: Name? = null) : ConeUnresolvedError() {
    override val qualifier: String? get() = name?.asString()
    override val reason: String get() = "Unresolved reference" + if (name != null) ": ${name.asString()}" else ""
}

class ConeUnresolvedSymbolError(val classId: ClassId) : ConeUnresolvedError() {
    override val qualifier: String get() = classId.asSingleFqName().asString()
    override val reason: String get() = "Symbol not found for $classId"
}

class ConeUnresolvedQualifierError(override val qualifier: String) : ConeUnresolvedError() {
    override val reason: String get() = "Symbol not found for $qualifier"
}

class ConeUnresolvedNameError(val name: Name) : ConeUnresolvedError() {
    override val qualifier: String get() = name.asString()
    override val reason: String get() = "Unresolved name: $name"
}

class ConeHiddenCandidateError(
    val candidateSymbol: AbstractFirBasedSymbol<*>
) : ConeDiagnostic() {
    override val reason: String get() = "HIDDEN: ${describeSymbol(candidateSymbol)} is invisible"
}

class ConeInapplicableCandidateError(
    val applicability: CandidateApplicability,
    val candidate: Candidate,
) : ConeDiagnostic() {
    override val reason: String get() = "Inapplicable($applicability): ${describeSymbol(candidate.symbol)}"
}

class ConeConstraintSystemHasContradiction(
    val candidate: Candidate,
) : ConeDiagnostic() {
    override val reason: String get() = "CS errors: ${describeSymbol(candidate.symbol)}"
}

class ConeArgumentTypeMismatchCandidateError(
    val expectedType: ConeKotlinType, val actualType: ConeKotlinType
) : ConeDiagnostic() {
    override val reason: String
        get() = "Type mismatch. Expected: $expectedType, Actual: $actualType"
}

class ConeAmbiguityError(val name: Name, val applicability: CandidateApplicability, val candidates: Collection<Candidate>) :
    ConeDiagnostic() {
    override val reason: String get() = "Ambiguity: $name, ${candidates.map { describeSymbol(it.symbol) }}"
}

class ConeOperatorAmbiguityError(val candidates: Collection<AbstractFirBasedSymbol<*>>) : ConeDiagnostic() {
    override val reason: String get() = "Operator overload ambiguity. Compatible candidates: ${candidates.map { describeSymbol(it) }}"
}

class ConeVariableExpectedError : ConeDiagnostic() {
    override val reason: String get() = "Variable expected"
}

class ConeValReassignmentError(val variable: FirVariableSymbol<*>) : ConeDiagnostic() {
    override val reason: String get() = "Re-assigning a val variable"
}

class ConeContractDescriptionError(override val reason: String) : ConeDiagnostic()

class ConeIllegalAnnotationError(val name: Name) : ConeDiagnostic() {
    override val reason: String get() = "Not a legal annotation: $name"
}

abstract class ConeUnmatchedTypeArgumentsError(val desiredCount: Int, val type: FirClassLikeSymbol<*>) : ConeDiagnostic()

class ConeWrongNumberOfTypeArgumentsError(
    desiredCount: Int,
    type: FirClassLikeSymbol<*>
) : ConeUnmatchedTypeArgumentsError(desiredCount, type) {
    override val reason: String get() = "Wrong number of type arguments"
}

class ConeNoTypeArgumentsOnRhsError(
    desiredCount: Int,
    type: FirClassLikeSymbol<*>
) : ConeUnmatchedTypeArgumentsError(desiredCount, type) {
    override val reason: String get() = "No type arguments on RHS"
}

class ConeInstanceAccessBeforeSuperCall(val target: String) : ConeDiagnostic() {
    override val reason: String get() = "Cannot access ''${target}'' before superclass constructor has been called"
}

class ConeUnsupportedCallableReferenceTarget(val fir: FirCallableDeclaration<*>) : ConeDiagnostic() {
    override val reason: String get() = "Unsupported declaration for callable reference: ${fir.render()}"
}

class ConeTypeParameterSupertype(val symbol: FirTypeParameterSymbol) : ConeDiagnostic() {
    override val reason: String get() = "Type parameter ${symbol.fir.name} cannot be a supertype"
}

class ConeTypeParameterInQualifiedAccess(val symbol: FirTypeParameterSymbol) : ConeDiagnostic() {
    override val reason: String get() = "Type parameter ${symbol.fir.name} in qualified access"
}

class ConeCyclicTypeBound(val symbol: FirTypeParameterSymbol, val bounds: ImmutableList<FirTypeRef>) : ConeDiagnostic() {
    override val reason: String get() = "Type parameter ${symbol.fir.name} has cyclic bounds"
}

class ConeImportFromSingleton(val name: Name) : ConeDiagnostic() {
    override val reason: String get() = "Import from singleton $name is not allowed"
}

class ConeUnsupportedDynamicType() : ConeDiagnostic() {
    override val reason: String get() = "Dynamic types are not supported in this context"
}

private fun describeSymbol(symbol: AbstractFirBasedSymbol<*>): String {
    return when (symbol) {
        is FirClassLikeSymbol<*> -> symbol.classId.asString()
        is FirCallableSymbol<*> -> symbol.callableId.toString()
        else -> "$symbol"
    }
}
