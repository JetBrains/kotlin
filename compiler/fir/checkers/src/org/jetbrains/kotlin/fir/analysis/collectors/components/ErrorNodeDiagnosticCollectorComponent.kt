/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind.*
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef

class ErrorNodeDiagnosticCollectorComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        val source = errorLoop.source ?: return
        runCheck { reportFirDiagnostic(errorLoop.diagnostic, source, it) }
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        val source = errorTypeRef.source ?: return
        runCheck { reportFirDiagnostic(errorTypeRef.diagnostic, source, it) }
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: CheckerContext) {
        val source = errorNamedReference.source ?: return
        runCheck { reportFirDiagnostic(errorNamedReference.diagnostic, source, it) }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: CheckerContext) {
        val source = errorExpression.source ?: return
        runCheck { reportFirDiagnostic(errorExpression.diagnostic, source, it) }
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction, data: CheckerContext) {
        val source = errorFunction.source ?: return
        runCheck { reportFirDiagnostic(errorFunction.diagnostic, source, it) }
    }

    private fun reportFirDiagnostic(diagnostic: ConeDiagnostic, source: FirSourceElement, reporter: DiagnosticReporter) {
        val coneDiagnostic = when (diagnostic) {
            is ConeUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.on(source, diagnostic.name?.asString() ?: "<No name>")
            is ConeUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.on(source, diagnostic.classId.asString())
            is ConeUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.on(source, diagnostic.name.asString())
            is ConeInapplicableCandidateError -> FirErrors.INAPPLICABLE_CANDIDATE.on(source, diagnostic.candidates.map { it.symbol })
            is ConeAmbiguityError -> FirErrors.AMBIGUITY.on(source, diagnostic.candidates)
            is ConeOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.on(source, diagnostic.candidates)
            is ConeVariableExpectedError -> FirErrors.VARIABLE_EXPECTED.on(source)
            is ConeTypeMismatchError -> FirErrors.TYPE_MISMATCH.on(source, diagnostic.expectedType, diagnostic.actualType)
            is ConeSimpleDiagnostic -> diagnostic.getFactory().on(source)
            is ConeStubDiagnostic -> null
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
            IllegalConstExpression -> FirErrors.ILLEGAL_CONST_EXPRESSION
            IllegalUnderscore -> FirErrors.ILLEGAL_UNDERSCORE
            DeserializationError -> FirErrors.DESERIALIZATION_ERROR
            InferenceError -> FirErrors.INFERENCE_ERROR
            TypeParameterAsSupertype -> FirErrors.TYPE_PARAMETER_AS_SUPERTYPE
            EnumAsSupertype -> FirErrors.ENUM_AS_SUPERTYPE
            RecursionInSupertypes -> FirErrors.RECURSION_IN_SUPERTYPES
            RecursionInImplicitTypes -> FirErrors.RECURSION_IN_IMPLICIT_TYPES
            SuperNotAllowed -> FirErrors.SUPER_IS_NOT_AN_EXPRESSION
            Java -> FirErrors.ERROR_FROM_JAVA_RESOLUTION
            ExpressionRequired -> FirErrors.EXPRESSION_REQUIRED
            JumpOutsideLoop -> FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
            NotLoopLabel -> FirErrors.NOT_A_LOOP_LABEL
            VariableExpected -> FirErrors.VARIABLE_EXPECTED
            Other -> FirErrors.OTHER_ERROR
            else -> throw IllegalArgumentException("Unsupported diagnostic kind: $kind at $javaClass")
        }
    }
}
