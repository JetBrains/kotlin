/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.isInfix
import org.jetbrains.kotlin.fir.declarations.isOperator
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private fun ConeDiagnostic.toFirDiagnostic(
    source: FirSourceElement,
    qualifiedAccessSource: FirSourceElement?
): FirDiagnostic<FirSourceElement>? = when (this) {
    is ConeUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.name?.asString() ?: "<No name>")
    is ConeUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.classId.asString())
    is ConeUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.name.asString())
    is ConeUnresolvedQualifierError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.qualifier)
    is ConeHiddenCandidateError -> FirErrors.INVISIBLE_REFERENCE.on(source, this.candidateSymbol)
    is ConeAmbiguityError -> if (this.applicability.isSuccess) {
        FirErrors.OVERLOAD_RESOLUTION_AMBIGUITY.on(source, this.candidates.map { it.symbol })
    } else if (this.applicability == CandidateApplicability.UNSAFE_CALL) {
        val candidate = candidates.first { it.currentApplicability == CandidateApplicability.UNSAFE_CALL }
        val unsafeCall = candidate.diagnostics.firstIsInstance<UnsafeCall>()
        mapUnsafeCallError(candidate, unsafeCall, source, qualifiedAccessSource)
    } else {
        FirErrors.NONE_APPLICABLE.on(source, this.candidates.map { it.symbol })
    }
    is ConeOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.on(source, this.candidates)
    is ConeVariableExpectedError -> FirErrors.VARIABLE_EXPECTED.on(source)
    is ConeValReassignmentError -> when (val symbol = this.variable) {
        is FirBackingFieldSymbol -> FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR.on(source, symbol.fir.symbol)
        else -> FirErrors.VAL_REASSIGNMENT.on(source, symbol)
    }
    is ConeTypeMismatchError -> FirErrors.TYPE_MISMATCH.on(qualifiedAccessSource ?: source, this.expectedType, this.actualType)
    is ConeUnexpectedTypeArgumentsError -> FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.on(this.source.safeAs() ?: source)
    is ConeIllegalAnnotationError -> FirErrors.NOT_AN_ANNOTATION_CLASS.on(source, this.name.asString())
    is ConeWrongNumberOfTypeArgumentsError ->
        FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(qualifiedAccessSource ?: source, this.desiredCount, this.type)
    is ConeNoTypeArgumentsOnRhsError ->
        FirErrors.NO_TYPE_ARGUMENTS_ON_RHS.on(qualifiedAccessSource ?: source, this.desiredCount, this.type)
    is ConeSimpleDiagnostic -> when (source.kind) {
        is FirFakeSourceElementKind -> null
        else -> this.getFactory(source).on(qualifiedAccessSource ?: source)
    }
    is ConeInstanceAccessBeforeSuperCall -> FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.on(source, this.target)
    is ConeStubDiagnostic -> null
    is ConeIntermediateDiagnostic -> null
    is ConeContractDescriptionError -> FirErrors.ERROR_IN_CONTRACT_DESCRIPTION.on(source, this.reason)
    is ConeTypeParameterSupertype -> FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE.on(source, this.reason)
    is ConeTypeParameterInQualifiedAccess -> null // reported in various checkers instead
    is ConeNotAnnotationContainer -> null
    else -> throw IllegalArgumentException("Unsupported diagnostic type: ${this.javaClass}")
}

fun ConeDiagnostic.toFirDiagnostics(
    source: FirSourceElement,
    qualifiedAccessSource: FirSourceElement?
): List<FirDiagnostic<FirSourceElement>> {
    if (this is ConeInapplicableCandidateError) {
        return mapInapplicableCandidateError(this, source, qualifiedAccessSource)
    }
    return listOfNotNull(toFirDiagnostic(source, qualifiedAccessSource))
}

private fun mapUnsafeCallError(
    candidate: Candidate,
    rootCause: UnsafeCall,
    source: FirSourceElement,
    qualifiedAccessSource: FirSourceElement?,
): FirDiagnostic<*> {
    if (candidate.callInfo.isImplicitInvoke) {
        return FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL.on(source, rootCause.actualType)
    }

    val candidateFunction = candidate.symbol.fir as? FirSimpleFunction
    val candidateFunctionName = candidateFunction?.name
    val left = candidate.callInfo.explicitReceiver
    val right = candidate.callInfo.argumentList.arguments.singleOrNull()
    if (left != null && right != null &&
        source.elementType == KtNodeTypes.OPERATION_REFERENCE &&
        (candidateFunction?.isOperator == true || candidateFunction?.isInfix == true)
    ) {
        val operationToken = source.getChild(KtTokens.IDENTIFIER)
        if (candidateFunction.isInfix && operationToken?.elementType == KtTokens.IDENTIFIER) {
            return FirErrors.UNSAFE_INFIX_CALL.on(source, left, candidateFunctionName!!.asString(), right)
        }
        if (candidateFunction.isOperator && operationToken == null) {
            return FirErrors.UNSAFE_OPERATOR_CALL.on(source, left, candidateFunctionName!!.asString(), right)
        }
    }
    return if (source.kind == FirFakeSourceElementKind.ArrayAccessNameReference) {
        FirErrors.UNSAFE_CALL.on(source, rootCause.actualType)
    } else {
        FirErrors.UNSAFE_CALL.on(qualifiedAccessSource ?: source, rootCause.actualType)
    }
}

private fun mapInapplicableCandidateError(
    diagnostic: ConeInapplicableCandidateError,
    source: FirSourceElement,
    qualifiedAccessSource: FirSourceElement?,
): List<FirDiagnostic<FirSourceElement>> {
    // TODO: Need to distinguish SMARTCAST_IMPOSSIBLE
    return diagnostic.candidate.diagnostics.filter { it.applicability == diagnostic.applicability }.mapNotNull { rootCause ->
        when (rootCause) {
            is VarargArgumentOutsideParentheses -> FirErrors.VARARG_OUTSIDE_PARENTHESES.on(
                rootCause.argument.source ?: qualifiedAccessSource
            )
            is NamedArgumentNotAllowed -> FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.on(
                rootCause.argument.source,
                rootCause.forbiddenNamedArgumentsTarget
            )
            is ArgumentTypeMismatch -> FirErrors.ARGUMENT_TYPE_MISMATCH.on(
                rootCause.argument.source ?: source,
                rootCause.expectedType,
                rootCause.actualType
            )
            is NullForNotNullType -> FirErrors.NULL_FOR_NONNULL_TYPE.on(
                rootCause.argument.source ?: source
            )
            is NonVarargSpread -> FirErrors.NON_VARARG_SPREAD.on(rootCause.argument.source?.getChild(KtTokens.MUL, depth = 1)!!)
            is ArgumentPassedTwice -> FirErrors.ARGUMENT_PASSED_TWICE.on(rootCause.argument.source)
            is TooManyArguments -> FirErrors.TOO_MANY_ARGUMENTS.on(rootCause.argument.source ?: source, rootCause.function)
            is NoValueForParameter -> FirErrors.NO_VALUE_FOR_PARAMETER.on(qualifiedAccessSource ?: source, rootCause.valueParameter)
            is NameNotFound -> FirErrors.NAMED_PARAMETER_NOT_FOUND.on(
                rootCause.argument.source ?: source,
                rootCause.argument.name.asString()
            )
            is UnsafeCall -> mapUnsafeCallError(diagnostic.candidate, rootCause, source, qualifiedAccessSource)
            else -> null
        }
    }.ifEmpty { listOf(FirErrors.INAPPLICABLE_CANDIDATE.on(source, diagnostic.candidate.symbol)) }
}

private fun ConeSimpleDiagnostic.getFactory(source: FirSourceElement): FirDiagnosticFactory0<*> {
    @Suppress("UNCHECKED_CAST")
    return when (kind) {
        DiagnosticKind.Syntax -> FirErrors.SYNTAX
        DiagnosticKind.ReturnNotAllowed -> FirErrors.RETURN_NOT_ALLOWED
        DiagnosticKind.UnresolvedLabel -> FirErrors.UNRESOLVED_LABEL
        DiagnosticKind.NoThis -> FirErrors.NO_THIS
        DiagnosticKind.IllegalConstExpression -> FirErrors.ILLEGAL_CONST_EXPRESSION
        DiagnosticKind.IllegalUnderscore -> FirErrors.ILLEGAL_UNDERSCORE
        DiagnosticKind.DeserializationError -> FirErrors.DESERIALIZATION_ERROR
        DiagnosticKind.InferenceError -> FirErrors.INFERENCE_ERROR
        DiagnosticKind.EnumAsSupertype -> FirErrors.ENUM_AS_SUPERTYPE
        DiagnosticKind.RecursionInSupertypes -> FirErrors.RECURSION_IN_SUPERTYPES
        DiagnosticKind.RecursionInImplicitTypes -> FirErrors.RECURSION_IN_IMPLICIT_TYPES
        DiagnosticKind.Java -> FirErrors.ERROR_FROM_JAVA_RESOLUTION
        DiagnosticKind.SuperNotAllowed -> FirErrors.SUPER_IS_NOT_AN_EXPRESSION
        DiagnosticKind.ExpressionExpected -> when (source.elementType) {
            KtNodeTypes.BINARY_EXPRESSION -> FirErrors.ASSIGNMENT_IN_EXPRESSION_CONTEXT
            KtNodeTypes.FUN -> FirErrors.ANONYMOUS_FUNCTION_WITH_NAME
            else -> FirErrors.EXPRESSION_EXPECTED
        }
        DiagnosticKind.JumpOutsideLoop -> FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
        DiagnosticKind.NotLoopLabel -> FirErrors.NOT_A_LOOP_LABEL
        DiagnosticKind.VariableExpected -> FirErrors.VARIABLE_EXPECTED
        DiagnosticKind.ValueParameterWithNoTypeAnnotation -> FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION
        DiagnosticKind.CannotInferParameterType -> FirErrors.CANNOT_INFER_PARAMETER_TYPE
        DiagnosticKind.UnknownCallableKind -> FirErrors.UNKNOWN_CALLABLE_KIND
        DiagnosticKind.IllegalProjectionUsage -> FirErrors.ILLEGAL_PROJECTION_USAGE
        DiagnosticKind.MissingStdlibClass -> FirErrors.MISSING_STDLIB_CLASS
        DiagnosticKind.IntLiteralOutOfRange -> FirErrors.INT_LITERAL_OUT_OF_RANGE
        DiagnosticKind.FloatLiteralOutOfRange -> FirErrors.FLOAT_LITERAL_OUT_OF_RANGE
        DiagnosticKind.WrongLongSuffix -> FirErrors.WRONG_LONG_SUFFIX
        DiagnosticKind.Other -> FirErrors.OTHER_ERROR
        DiagnosticKind.IncorrectCharacterLiteral -> FirErrors.INCORRECT_CHARACTER_LITERAL
        DiagnosticKind.EmptyCharacterLiteral -> FirErrors.EMPTY_CHARACTER_LITERAL
        DiagnosticKind.TooManyCharactersInCharacterLiteral -> FirErrors.TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL
        DiagnosticKind.IllegalEscape -> FirErrors.ILLEGAL_ESCAPE
        else -> throw IllegalArgumentException("Unsupported diagnostic kind: $kind at $javaClass")
    }
}
