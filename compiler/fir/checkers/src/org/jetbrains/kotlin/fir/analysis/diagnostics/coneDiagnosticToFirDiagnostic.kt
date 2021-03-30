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
import org.jetbrains.kotlin.fir.resolve.calls.InapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.calls.NamedArgumentNotAllowed
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionDiagnostic
import org.jetbrains.kotlin.fir.resolve.calls.VarargArgumentOutsideParentheses
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private fun ConeDiagnostic.toFirDiagnostic(
    source: FirSourceElement,
    qualifiedAccessSource: FirSourceElement?
): FirDiagnostic<FirSourceElement>? = when (this) {
    is ConeUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.name?.asString() ?: "<No name>")
    is ConeUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.classId.asString())
    is ConeUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.name.asString())
    is ConeUnresolvedQualifierError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.qualifier)
    is ConeHiddenCandidateError -> FirErrors.HIDDEN.on(source, this.candidateSymbol)
    is ConeAmbiguityError -> if (!this.applicability.isSuccess) {
        FirErrors.NONE_APPLICABLE.on(source, this.candidates)
    } else {
        FirErrors.AMBIGUITY.on(source, this.candidates)
    }
    is ConeOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.on(source, this.candidates)
    is ConeVariableExpectedError -> FirErrors.VARIABLE_EXPECTED.on(source)
    is ConeTypeMismatchError -> FirErrors.TYPE_MISMATCH.on(qualifiedAccessSource ?: source, this.expectedType, this.actualType)
    is ConeUnexpectedTypeArgumentsError -> FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.on(this.source.safeAs() ?: source)
    is ConeIllegalAnnotationError -> FirErrors.NOT_AN_ANNOTATION_CLASS.on(source, this.name.asString())
    is ConeWrongNumberOfTypeArgumentsError ->
        FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(qualifiedAccessSource ?: source, this.desiredCount, this.type)
    is ConeNoTypeArgumentsOnRhsError ->
        FirErrors.NO_TYPE_ARGUMENTS_ON_RHS.on(qualifiedAccessSource ?: source, this.desiredCount, this.type)
    is ConeSimpleDiagnostic -> when {
        source.kind is FirFakeSourceElementKind -> null
        else -> this.getFactory().on(qualifiedAccessSource ?: source)
    }
    is ConeInstanceAccessBeforeSuperCall -> FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.on(source, this.target)
    is ConeStubDiagnostic -> null
    is ConeIntermediateDiagnostic -> null
    is ConeContractDescriptionError -> FirErrors.ERROR_IN_CONTRACT_DESCRIPTION.on(source, this.reason)
    is ConeTypeParameterSupertype -> FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE.on(source, this.reason)
    is ConeTypeParameterInQualifiedAccess -> null // reported in various checkers instead
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

private fun ConeKotlinType.isEffectivelyNotNull(): Boolean {
    return when (this) {
        is ConeClassLikeType -> !isMarkedNullable
        is ConeTypeParameterType -> !isMarkedNullable && lookupTag.typeParameterSymbol.fir.bounds.any {
            it.coneTypeSafe<ConeKotlinType>()?.isEffectivelyNotNull() == true
        }
        else -> false
    }
}

private fun mapUnsafeCallError(
    diagnostic: ConeInapplicableCandidateError,
    source: FirSourceElement,
    rootCause: ResolutionDiagnostic?,
    qualifiedAccessSource: FirSourceElement?,
): FirDiagnostic<*>? {
    if (rootCause !is InapplicableWrongReceiver) return null
    val actualType = rootCause.actualType ?: return null
    val expectedType = rootCause.expectedType
    if (actualType.isNullable && (expectedType == null || expectedType.isEffectivelyNotNull())) {
        if (diagnostic.candidate.callInfo.isImplicitInvoke) {
            return FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL.on(source, actualType)
        }

        val candidateFunction = diagnostic.candidate.symbol.fir as? FirSimpleFunction
        val candidateFunctionName = candidateFunction?.name
        val left = diagnostic.candidate.callInfo.explicitReceiver
        val right = diagnostic.candidate.callInfo.argumentList.arguments.singleOrNull()
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
            FirErrors.UNSAFE_CALL.on(source, actualType)
        } else {
            FirErrors.UNSAFE_CALL.on(qualifiedAccessSource ?: source, actualType)
        }
    }
    return null
}

private fun mapInapplicableCandidateError(
    diagnostic: ConeInapplicableCandidateError,
    source: FirSourceElement,
    qualifiedAccessSource: FirSourceElement?,
): List<FirDiagnostic<FirSourceElement>> {
    // TODO: Need to distinguish SMARTCAST_IMPOSSIBLE
    return diagnostic.candidate.diagnostics.filter { it.applicability == diagnostic.applicability }.mapNotNull { rootCause ->
        mapUnsafeCallError(diagnostic, source, rootCause, qualifiedAccessSource)?.let { return@mapNotNull it }

        when (rootCause) {
            is VarargArgumentOutsideParentheses -> FirErrors.VARARG_OUTSIDE_PARENTHESES.on(
                rootCause.argument.source ?: qualifiedAccessSource
            )
            is NamedArgumentNotAllowed -> FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.on(
                rootCause.argument.source ?: qualifiedAccessSource,
                rootCause.forbiddenNamedArgumentsTarget
            )
            else -> null
        }
    }.ifEmpty { listOf(FirErrors.INAPPLICABLE_CANDIDATE.on(source, diagnostic.candidate.symbol)) }
}

private fun ConeSimpleDiagnostic.getFactory(): FirDiagnosticFactory0<FirSourceElement, *> {
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
        DiagnosticKind.ExpressionRequired -> FirErrors.EXPRESSION_REQUIRED
        DiagnosticKind.JumpOutsideLoop -> FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
        DiagnosticKind.NotLoopLabel -> FirErrors.NOT_A_LOOP_LABEL
        DiagnosticKind.VariableExpected -> FirErrors.VARIABLE_EXPECTED
        DiagnosticKind.ValueParameterWithNoTypeAnnotation -> FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION
        DiagnosticKind.UnknownCallableKind -> FirErrors.UNKNOWN_CALLABLE_KIND
        DiagnosticKind.IllegalProjectionUsage -> FirErrors.ILLEGAL_PROJECTION_USAGE
        DiagnosticKind.MissingStdlibClass -> FirErrors.MISSING_STDLIB_CLASS
        DiagnosticKind.Other -> FirErrors.OTHER_ERROR
        else -> throw IllegalArgumentException("Unsupported diagnostic kind: $kind at $javaClass")
    }
}
