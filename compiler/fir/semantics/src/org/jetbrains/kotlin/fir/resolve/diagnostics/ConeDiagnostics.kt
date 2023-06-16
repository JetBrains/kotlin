/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics

import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionElement
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnosticWithNullability
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnosticWithSource
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.AbstractCandidate
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

sealed interface ConeUnresolvedError : ConeDiagnostic {
    val qualifier: String
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

class ConeUnresolvedReferenceError(val name: Name) : ConeUnresolvedError {
    override val qualifier: String get() = if (!name.isSpecial) name.asString() else "NO_NAME"
    override val reason: String get() = "Unresolved reference: ${name.asString()}"
}

class ConeUnresolvedSymbolError(val classId: ClassId) : ConeUnresolvedError {
    override val qualifier: String get() = classId.asSingleFqName().asString()
    override val reason: String get() = "Symbol not found for $classId"
}

class ConeUnresolvedTypeQualifierError(val qualifiers: List<FirQualifierPart>, override val isNullable: Boolean)
    : ConeUnresolvedError, ConeDiagnosticWithNullability {
    override val qualifier: String get() = qualifiers.joinToString(separator = ".") { it.name.asString() }
    override val reason: String get() = "Symbol not found for $qualifier${if (isNullable) "?" else ""}"
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

sealed class ConeContractDescriptionError : ConeDiagnostic {
    class IllegalElement(val element: FirElement) : ConeContractDescriptionError() {
        override val reason: String
            get() = "Illegal element in contract description"
    }

    class UnresolvedCall(val name: Name) : ConeContractDescriptionError() {
        override val reason: String
            get() = "Unresolved call in contract description: ${name.asString()}"
    }

    class NoReceiver(val name: Name) : ConeContractDescriptionError() {
        override val reason: String
            get() = "No receiver for call ${name.asString()} found"
    }

    class NoArgument(val name: Name) : ConeContractDescriptionError() {
        override val reason: String
            get() = "No argument for call ${name.asString()} found"
    }

    class NotAConstant(val element: Any) : ConeContractDescriptionError() {
        override val reason: String
            get() = "$element is not a constant reference"
    }

    class IllegalConst(
        val element: FirConstExpression<*>,
        val onlyNullAllowed: Boolean
    ) : ConeContractDescriptionError() {
        override val reason: String
            get() = buildString {
                append(element.render())
                append("is not a null")
                if (!onlyNullAllowed) {
                    append(", true or false")
                }
            }
    }

    class NotAParameterReference(val element: ConeContractDescriptionElement) : ConeContractDescriptionError() {
        override val reason: String
            get() = "$element is not a parameter or receiver reference"
    }

    class IllegalParameter(val symbol: FirCallableSymbol<*>, override val reason: String) : ConeContractDescriptionError()

    class UnresolvedThis(val expression: FirThisReceiverExpression) : ConeContractDescriptionError() {
        override val reason: String
            get() = "Can't resolve this reference"
    }

    class IllegalThis(val expression: FirThisReceiverExpression) : ConeContractDescriptionError() {
        override val reason: String
            get() = "Only this reference to extension receiver of a function is allowed"
    }

    class UnresolvedInvocationKind(val element: FirElement) : ConeContractDescriptionError() {
        override val reason: String
            get() = "${element.render()} is not a valid invocation kind"
    }

    class NotABooleanExpression(val element: ConeContractDescriptionElement) : ConeContractDescriptionError() {
        override val reason: String
            get() = "$element is not a boolean expression"
    }

    class NotContractDsl(val callableId: CallableId) : ConeContractDescriptionError() {
        override val reason: String
            get() = "$callableId is not a part of contracts DSL"
    }

    class IllegalEqualityOperator(val operation: FirOperation) : ConeContractDescriptionError() {
        override val reason: String
            get() = "$operation operator call is illegal in contract description"
    }

    class NotSelfTypeParameter(val symbol: FirTypeParameterSymbol) : ConeContractDescriptionError() {
        override val reason: String
            get() = "Type parameter ${symbol.name} does not belong to owner of contract"
    }

    class NotReifiedTypeParameter(val symbol: FirTypeParameterSymbol) : ConeContractDescriptionError() {
        override val reason: String
            get() = "Type parameter ${symbol.name} is not reified"
    }
}

class ConeIllegalAnnotationError(val name: Name) : ConeDiagnostic {
    override val reason: String get() = "Not a legal annotation: $name"
}

sealed interface ConeUnmatchedTypeArgumentsError : ConeDiagnosticWithSymbol<FirClassLikeSymbol<*>> {
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
    override val reason: String get() = "Cannot access ''${target}'' before the instance has been initialized"
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

open class ConeUnsupportedDefaultValueInFunctionType(source: KtSourceElement? = null) :
    ConeUnsupported("Default value of parameter in function type", source)

class ConeUnresolvedParentInImport(val parentClassId: ClassId) : ConeDiagnostic {
    override val reason: String
        get() = "unresolved import"
}

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

class ConeAmbiguousAlteredAssign(val altererNames: List<String?>) : ConeDiagnostic {
    override val reason: String
        get() = "Assign altered by multiple extensions"
}

object ConeForbiddenIntersection : ConeDiagnostic {
    override val reason: String get() = "Such an intersection type is not allowed"
}

class ConeAmbiguouslyResolvedAnnotationFromPlugin(
    val typeFromCompilerPhase: ConeKotlinType,
    val typeFromTypesPhase: ConeKotlinType
) : ConeDiagnostic {
    override val reason: String
        get() = """
            Annotation type resolved differently on compiler annotation and types stages:
              - compiler annotations: $typeFromCompilerPhase
              - types stage: $typeFromTypesPhase
        """
}

class ConeAmbiguouslyResolvedAnnotationArgument(
    val symbolFromCompilerPhase: FirBasedSymbol<*>,
    val symbolFromAnnotationArgumentsPhase: FirBasedSymbol<*>?
) : ConeDiagnostic {
    override val reason: String
        get() = """
            Annotation symbol resolved differently on compiler annotation and symbols stages:
              - compiler annotations: $symbolFromCompilerPhase
              - compiler arguments stage: $symbolFromAnnotationArgumentsPhase
        """
}
