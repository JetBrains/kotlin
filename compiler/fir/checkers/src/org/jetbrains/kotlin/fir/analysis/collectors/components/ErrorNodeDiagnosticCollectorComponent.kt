/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind.*
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.CandidateApplicability
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ErrorNodeDiagnosticCollectorComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        val source = errorLoop.source ?: return
        reportFirDiagnostic(errorLoop.diagnostic, source, reporter)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        val source = errorTypeRef.source ?: return
        reportFirDiagnostic(errorTypeRef.diagnostic, source, reporter)
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: CheckerContext) {
        val source = errorNamedReference.source ?: return
        // Don't report duplicated unresolved reference on annotation entry (already reported on its type)
        if (source.elementType == KtNodeTypes.ANNOTATION_ENTRY && errorNamedReference.diagnostic is ConeUnresolvedNameError) return
        reportFirDiagnostic(errorNamedReference.diagnostic, source, reporter)
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: CheckerContext) {
        val source = errorExpression.source ?: return
        reportFirDiagnostic(errorExpression.diagnostic, source, reporter)
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction, data: CheckerContext) {
        val source = errorFunction.source ?: return
        reportFirDiagnostic(errorFunction.diagnostic, source, reporter)
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: CheckerContext) {
        val source = errorResolvedQualifier.source ?: return
        reportFirDiagnostic(errorResolvedQualifier.diagnostic, source, reporter)
    }

    private fun reportFirDiagnostic(diagnostic: ConeDiagnostic, source: FirSourceElement, reporter: DiagnosticReporter) {
        val coneDiagnostic = when (diagnostic) {
            is ConeUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.on(source, diagnostic.name?.asString() ?: "<No name>")
            is ConeUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.on(source, diagnostic.classId.asString())
            is ConeUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.on(source, diagnostic.name.asString())
            is ConeHiddenCandidateError -> FirErrors.HIDDEN.on(source, diagnostic.candidateSymbol)
            is ConeInapplicableCandidateError -> FirErrors.INAPPLICABLE_CANDIDATE.on(source, diagnostic.candidateSymbol)
            is ConeAmbiguityError -> if (diagnostic.applicability < CandidateApplicability.SYNTHETIC_RESOLVED) {
                FirErrors.NONE_APPLICABLE.on(source, diagnostic.candidates)
            } else {
                FirErrors.AMBIGUITY.on(source, diagnostic.candidates)
            }
            is ConeOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.on(source, diagnostic.candidates)
            is ConeVariableExpectedError -> FirErrors.VARIABLE_EXPECTED.on(source)
            is ConeTypeMismatchError -> FirErrors.TYPE_MISMATCH.on(source, diagnostic.expectedType, diagnostic.actualType)
            is ConeUnexpectedTypeArgumentsError -> FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.on(diagnostic.source.safeAs() ?: source)
            is ConeIllegalAnnotationError -> FirErrors.NOT_AN_ANNOTATION_CLASS.on(source, diagnostic.name.asString())
            is ConeWrongNumberOfTypeArgumentsError ->
                FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(source, diagnostic.desiredCount, diagnostic.type)
            is ConeSimpleDiagnostic -> when {
                source.kind is FirFakeSourceElementKind -> null
                diagnostic.kind == SymbolNotFound -> FirErrors.UNRESOLVED_REFERENCE.on(source, "<No name>")
                else -> diagnostic.getFactory().on(source)
            }
            is ConeInstanceAccessBeforeSuperCall -> FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.on(source, diagnostic.target)
            is ConeStubDiagnostic -> null
            is ConeIntermediateDiagnostic -> null
            else -> throw IllegalArgumentException("Unsupported diagnostic type: ${diagnostic.javaClass}")
        }
        reporter.report(coneDiagnostic)
    }

    private fun ConeSimpleDiagnostic.getFactory(): FirDiagnosticFactory0<FirSourceElement, *> {
        @Suppress("UNCHECKED_CAST")
        return when (kind) {
            Syntax -> FirErrors.SYNTAX_ERROR
            ReturnNotAllowed -> FirErrors.RETURN_NOT_ALLOWED
            UnresolvedLabel -> FirErrors.UNRESOLVED_LABEL
            NoThis -> FirErrors.NO_THIS
            IllegalConstExpression -> FirErrors.ILLEGAL_CONST_EXPRESSION
            IllegalUnderscore -> FirErrors.ILLEGAL_UNDERSCORE
            DeserializationError -> FirErrors.DESERIALIZATION_ERROR
            InferenceError -> FirErrors.INFERENCE_ERROR
            TypeParameterAsSupertype -> FirErrors.TYPE_PARAMETER_AS_SUPERTYPE
            EnumAsSupertype -> FirErrors.ENUM_AS_SUPERTYPE
            RecursionInSupertypes -> FirErrors.RECURSION_IN_SUPERTYPES
            RecursionInImplicitTypes -> FirErrors.RECURSION_IN_IMPLICIT_TYPES
            Java -> FirErrors.ERROR_FROM_JAVA_RESOLUTION
            SuperNotAllowed -> FirErrors.SUPER_IS_NOT_AN_EXPRESSION
            ExpressionRequired -> FirErrors.EXPRESSION_REQUIRED
            JumpOutsideLoop -> FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
            NotLoopLabel -> FirErrors.NOT_A_LOOP_LABEL
            VariableExpected -> FirErrors.VARIABLE_EXPECTED
            NoTypeForTypeParameter -> FirErrors.NO_TYPE_FOR_TYPE_PARAMETER
            UnknownCallableKind -> FirErrors.UNKNOWN_CALLABLE_KIND
            IllegalProjectionUsage -> FirErrors.ILLEGAL_PROJECTION_USAGE
            MissingStdlibClass -> FirErrors.MISSING_STDLIB_CLASS
            Other -> FirErrors.OTHER_ERROR
            else -> throw IllegalArgumentException("Unsupported diagnostic kind: $kind at $javaClass")
        }
    }
}
