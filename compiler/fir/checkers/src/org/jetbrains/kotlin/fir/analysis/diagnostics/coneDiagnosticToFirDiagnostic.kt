/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.calls.InapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun ConeDiagnostic.toFirDiagnostic(source: FirSourceElement): FirDiagnostic<FirSourceElement>? = when (this) {
    is ConeUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.name?.asString() ?: "<No name>")
    is ConeUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.classId.asString())
    is ConeUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.on(source, this.name.asString())
    is ConeHiddenCandidateError -> FirErrors.HIDDEN.on(source, this.candidateSymbol)
    is ConeInapplicableCandidateError -> mapInapplicableCandidateError(this, source)
    is ConeAmbiguityError -> if (!this.applicability.isSuccess) {
        FirErrors.NONE_APPLICABLE.on(source, this.candidates)
    } else {
        FirErrors.AMBIGUITY.on(source, this.candidates)
    }
    is ConeOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.on(source, this.candidates)
    is ConeVariableExpectedError -> FirErrors.VARIABLE_EXPECTED.on(source)
    is ConeTypeMismatchError -> FirErrors.TYPE_MISMATCH.on(source, this.expectedType, this.actualType)
    is ConeUnexpectedTypeArgumentsError -> FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.on(this.source.safeAs() ?: source)
    is ConeIllegalAnnotationError -> FirErrors.NOT_AN_ANNOTATION_CLASS.on(source, this.name.asString())
    is ConeWrongNumberOfTypeArgumentsError ->
        FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(source, this.desiredCount, this.type)
    is ConeSimpleDiagnostic -> when {
        source.kind is FirFakeSourceElementKind -> null
        this.kind == DiagnosticKind.SymbolNotFound -> FirErrors.UNRESOLVED_REFERENCE.on(source, "<No name>")
        else -> this.getFactory().on(source)
    }
    is ConeInstanceAccessBeforeSuperCall -> FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.on(source, this.target)
    is ConeStubDiagnostic -> null
    is ConeIntermediateDiagnostic -> null
    else -> throw IllegalArgumentException("Unsupported diagnostic type: ${this.javaClass}")
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

private fun mapInapplicableCandidateError(
    diagnostic: ConeInapplicableCandidateError,
    source: FirSourceElement,
): FirDiagnostic<*> {
    // TODO: Need to distinguish SMARTCAST_IMPOSSIBLE
    // TODO: handle other UNSAFE_* variants: invoke, infix, operator
    val rootCause = diagnostic.diagnostics.find { it.applicability == diagnostic.applicability }
    if (rootCause != null &&
        rootCause is InapplicableWrongReceiver &&
        rootCause.actualType?.isNullable == true &&
        (rootCause.expectedType == null || !rootCause.expectedType!!.isMarkedNullable)
    ) {
        val expectedType = rootCause.expectedType

        if (expectedType == null || expectedType.isEffectivelyNotNull()) {
            return FirErrors.UNSAFE_CALL.on(source, rootCause.actualType!!)
        }
    }
    return FirErrors.INAPPLICABLE_CANDIDATE.on(source, diagnostic.candidateSymbol)
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
        DiagnosticKind.TypeParameterAsSupertype -> FirErrors.TYPE_PARAMETER_AS_SUPERTYPE
        DiagnosticKind.EnumAsSupertype -> FirErrors.ENUM_AS_SUPERTYPE
        DiagnosticKind.RecursionInSupertypes -> FirErrors.RECURSION_IN_SUPERTYPES
        DiagnosticKind.RecursionInImplicitTypes -> FirErrors.RECURSION_IN_IMPLICIT_TYPES
        DiagnosticKind.Java -> FirErrors.ERROR_FROM_JAVA_RESOLUTION
        DiagnosticKind.SuperNotAllowed -> FirErrors.SUPER_IS_NOT_AN_EXPRESSION
        DiagnosticKind.ExpressionRequired -> FirErrors.EXPRESSION_REQUIRED
        DiagnosticKind.JumpOutsideLoop -> FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
        DiagnosticKind.NotLoopLabel -> FirErrors.NOT_A_LOOP_LABEL
        DiagnosticKind.VariableExpected -> FirErrors.VARIABLE_EXPECTED
        DiagnosticKind.NoTypeForTypeParameter -> FirErrors.NO_TYPE_FOR_TYPE_PARAMETER
        DiagnosticKind.UnknownCallableKind -> FirErrors.UNKNOWN_CALLABLE_KIND
        DiagnosticKind.IllegalProjectionUsage -> FirErrors.ILLEGAL_PROJECTION_USAGE
        DiagnosticKind.MissingStdlibClass -> FirErrors.MISSING_STDLIB_CLASS
        DiagnosticKind.Other -> FirErrors.OTHER_ERROR
        else -> throw IllegalArgumentException("Unsupported diagnostic kind: $kind at $javaClass")
    }
}