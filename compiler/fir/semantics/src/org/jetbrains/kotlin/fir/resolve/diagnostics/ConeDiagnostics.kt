/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics

import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnosticWithSource
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCandidate
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

sealed interface ConeUnresolvedError : ConeDiagnostic {
    val qualifier: String?
}

interface ConeDiagnosticWithSymbol<S : FirBasedSymbol<*>> : ConeDiagnostic {
    val symbol: S
}

interface ConeDiagnosticWithCandidates : ConeDiagnostic {
    val candidates: Collection<AbstractCandidate>
    val candidateSymbols: Collection<FirBasedSymbol<*>> get() = candidates.map { it.symbol }
}

interface ConeDiagnosticWithSingleCandidate : ConeDiagnosticWithCandidates {
    val candidate: AbstractCandidate
    val candidateSymbol: FirBasedSymbol<*> get() = candidate.symbol
    override val candidates: Collection<AbstractCandidate> get() = listOf(candidate)
    override val candidateSymbols: Collection<FirBasedSymbol<*>> get() = listOf(candidateSymbol)
}

class ConeUnresolvedReferenceError(val name: Name? = null) : ConeUnresolvedError {
    override val qualifier: String? get() = name?.asString()
    override val reason: String get() = "Unresolved reference" + if (name != null) ": ${name.asString()}" else ""
}

class ConeUnresolvedSymbolError(val classId: ClassId) : ConeUnresolvedError {
    override val qualifier: String get() = classId.asSingleFqName().asString()
    override val reason: String get() = "Symbol not found for $classId"
}

class ConeUnresolvedQualifierError(override val qualifier: String) : ConeUnresolvedError {
    override val reason: String get() = "Symbol not found for $qualifier"
}

class ConeUnresolvedNameError(val name: Name) : ConeUnresolvedError {
    override val qualifier: String get() = name.asString()
    override val reason: String get() = "Unresolved name: $name"
}

class ConeFunctionCallExpectedError(
    val name: Name,
    val hasValueParameters: Boolean,
    override val candidates: Collection<AbstractCandidate>
) : ConeDiagnosticWithCandidates {
    override val reason: String get() = "Function call expected: $name(${if (hasValueParameters) "..." else ""})"
}

class ConeFunctionExpectedError(val expression: String, val type: ConeKotlinType) : ConeDiagnostic {
    override val reason: String get() = "Expression '$expression' of type '$type' cannot be invoked as a function"
}

class ConeResolutionToClassifierError(
    override val candidate: AbstractCandidate,
    override val candidateSymbol: FirRegularClassSymbol
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String get() = "Resolution to classifier"
}

class ConeHiddenCandidateError(
    override val candidate: AbstractCandidate
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String get() = "HIDDEN: ${describeSymbol(candidateSymbol)} is deprecated with DeprecationLevel.HIDDEN"
}

class ConeVisibilityError(
    override val symbol: FirBasedSymbol<*>
) : ConeDiagnosticWithSymbol<FirBasedSymbol<*>> {
    override val reason: String get() = "HIDDEN: ${describeSymbol(symbol)} is invisible"
}

class ConeInapplicableWrongReceiver(override val candidates: Collection<AbstractCandidate>) : ConeDiagnosticWithCandidates {
    override val reason: String
        get() = "None of the following candidates is applicable because of receiver type mismatch: ${
            candidateSymbols.map { describeSymbol(it) }
        }"
}

class ConeInapplicableCandidateError(
    val applicability: CandidateApplicability,
    override val candidate: AbstractCandidate,
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String get() = "Inapplicable($applicability): ${describeSymbol(candidateSymbol)}"
}

class ConeNoCompanionObject(
    override val candidate: AbstractCandidate
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String
        get() = "Classifier ''$candidateSymbol'' does not have a companion object, and thus must be initialized here"
}

class ConeConstraintSystemHasContradiction(
    override val candidate: AbstractCandidate,
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String get() = "CS errors: ${describeSymbol(candidateSymbol)}"
    override val candidateSymbol: FirBasedSymbol<*> get() = candidate.symbol
}

class ConeArgumentTypeMismatchCandidateError(
    val expectedType: ConeKotlinType, val actualType: ConeKotlinType
) : ConeDiagnostic {
    override val reason: String
        get() = "Type mismatch. Expected: $expectedType, Actual: $actualType"
}

class ConeAmbiguityError(
    val name: Name,
    val applicability: CandidateApplicability,
    override val candidates: Collection<AbstractCandidate>
) : ConeDiagnosticWithCandidates {
    override val reason: String get() = "Ambiguity: $name, ${candidateSymbols.map { describeSymbol(it) }}"
    override val candidateSymbols: Collection<FirBasedSymbol<*>> get() = candidates.map { it.symbol }
}

class ConeOperatorAmbiguityError(override val candidates: Collection<AbstractCandidate>) : ConeDiagnosticWithCandidates {
    override val reason: String get() = "Operator overload ambiguity. Compatible candidates: ${candidateSymbols.map { describeSymbol(it) }}"
}

object ConeVariableExpectedError : ConeDiagnostic {
    override val reason: String get() = "Variable expected"
}

class ConeValReassignmentError(val variable: FirVariableSymbol<*>) : ConeDiagnostic {
    override val reason: String get() = "Re-assigning a val variable"
}

class ConeContractDescriptionError(override val reason: String) : ConeDiagnostic

class ConeIllegalAnnotationError(val name: Name) : ConeDiagnostic {
    override val reason: String get() = "Not a legal annotation: $name"
}

interface ConeUnmatchedTypeArgumentsError : ConeDiagnosticWithSymbol<FirClassLikeSymbol<*>> {
    val desiredCount: Int
}

class ConeWrongNumberOfTypeArgumentsError(
    override val desiredCount: Int,
    override val symbol: FirClassLikeSymbol<*>,
    source: KtSourceElement
) : ConeDiagnosticWithSource(source), ConeUnmatchedTypeArgumentsError {
    override val reason: String get() = "Wrong number of type arguments"
}

class ConeNoTypeArgumentsOnRhsError(
    override val desiredCount: Int,
    override val symbol: FirClassLikeSymbol<*>
) : ConeUnmatchedTypeArgumentsError {
    override val reason: String get() = "No type arguments on RHS"
}

class ConeOuterClassArgumentsRequired(
    val symbol: FirClassLikeSymbol<*>,
) : ConeDiagnostic {
    override val reason: String = "Type arguments should be specified for an outer class"
}

class ConeInstanceAccessBeforeSuperCall(val target: String) : ConeDiagnostic {
    override val reason: String get() = "Cannot access ''${target}'' before superclass constructor has been called"
}

class ConeUnsupportedCallableReferenceTarget(override val candidate: AbstractCandidate) : ConeDiagnosticWithSingleCandidate {
    override val reason: String get() = "Unsupported declaration for callable reference: ${candidate.symbol.fir.render()}"
}

class ConeTypeParameterSupertype(val symbol: FirTypeParameterSymbol) : ConeDiagnostic {
    override val reason: String get() = "Type parameter ${symbol.fir.name} cannot be a supertype"
}

class ConeTypeParameterInQualifiedAccess(val symbol: FirTypeParameterSymbol) : ConeDiagnostic {
    override val reason: String get() = "Type parameter ${symbol.fir.name} in qualified access"
}

class ConeCyclicTypeBound(val symbol: FirTypeParameterSymbol, val bounds: ImmutableList<FirTypeRef>) : ConeDiagnostic {
    override val reason: String get() = "Type parameter ${symbol.fir.name} has cyclic bounds"
}

class ConeImportFromSingleton(val name: Name) : ConeDiagnostic {
    override val reason: String get() = "Import from singleton $name is not allowed"
}

open class ConeUnsupported(override val reason: String, val source: KtSourceElement? = null) : ConeDiagnostic

class ConeUnresolvedParentInImport(val parentClassId: ClassId) : ConeDiagnostic {
    override val reason: String
        get() = "unresolved import"
}

class ConeUnsupportedDynamicType : ConeUnsupported("Dynamic types are not supported in this context")

class ConeDeprecated(
    val source: KtSourceElement?,
    override val symbol: FirBasedSymbol<*>,
    val deprecationInfo: DeprecationInfo
) : ConeDiagnosticWithSymbol<FirBasedSymbol<*>> {
    override val reason: String get() = "Deprecated: ${deprecationInfo.message}"
}

class ConeLocalVariableNoTypeOrInitializer(val variable: FirVariable) : ConeDiagnostic {
    override val reason: String get() = "Cannot infer variable type without initializer / getter / delegate"
}

class ConePropertyAsOperator(val symbol: FirPropertySymbol) : ConeDiagnostic {
    override val reason: String get() = "Cannot use a property as an operator"
}

class ConeUnknownLambdaParameterTypeDiagnostic : ConeDiagnostic {
    override val reason: String get() = "Unknown return lambda parameter type"
}

private fun describeSymbol(symbol: FirBasedSymbol<*>): String {
    return when (symbol) {
        is FirClassLikeSymbol<*> -> symbol.classId.asString()
        is FirCallableSymbol<*> -> symbol.callableId.toString()
        else -> "$symbol"
    }
}
