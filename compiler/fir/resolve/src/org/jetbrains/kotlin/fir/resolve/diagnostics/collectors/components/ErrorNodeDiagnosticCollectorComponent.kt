/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirStubDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef

class ErrorNodeDiagnosticCollectorComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitErrorLoop(errorLoop: FirErrorLoop) {
        val source = errorLoop.source ?: return
        runCheck { reportFirDiagnostic(errorLoop.diagnostic, source, it) }
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        val source = errorTypeRef.source ?: return
        runCheck { reportFirDiagnostic(errorTypeRef.diagnostic, source, it) }
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference) {
        val source = errorNamedReference.source ?: return
        runCheck { reportFirDiagnostic(errorNamedReference.diagnostic, source, it) }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression) {
        val source = errorExpression.source ?: return
        runCheck { reportFirDiagnostic(errorExpression.diagnostic, source, it) }
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction) {
        val source = errorFunction.source ?: return
        runCheck { reportFirDiagnostic(errorFunction.diagnostic, source, it) }
    }

    private fun reportFirDiagnostic(diagnostic: FirDiagnostic, source: FirSourceElement, reporter: DiagnosticReporter) {
        val coneDiagnostic = when (diagnostic) {
            is FirUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.onSource(source, diagnostic.name?.asString())
            is FirUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.onSource(source, diagnostic.classId.asString())
            is FirUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.onSource(source, diagnostic.name.asString())
            is FirInapplicableCandidateError -> FirErrors.INAPPLICABLE_CANDIDATE.onSource(source, diagnostic.candidates.map { it.symbol })
            is FirAmbiguityError -> FirErrors.AMBIGUITY.onSource(source, diagnostic.candidates)
            is FirOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.onSource(source, diagnostic.candidates)
            is FirVariableExpectedError -> Errors.VARIABLE_EXPECTED.onSource(source)
            is FirSimpleDiagnostic -> diagnostic.getFactory().onSource(source)
            is FirStubDiagnostic -> null
            else -> throw IllegalArgumentException("Unsupported diagnostic type: ${diagnostic.javaClass}")
        }
        reporter.report(coneDiagnostic)
    }

    private fun FirSimpleDiagnostic.getFactory(): DiagnosticFactory0<PsiElement> {
        return when (kind) {
            DiagnosticKind.Syntax -> FirErrors.SYNTAX_ERROR
            DiagnosticKind.ReturnNotAllowed -> Errors.RETURN_NOT_ALLOWED
            DiagnosticKind.UnresolvedLabel -> FirErrors.UNRESOLVED_LABEL
            DiagnosticKind.IllegalConstExpression -> FirErrors.ILLEGAL_CONST_EXPRESSION
            DiagnosticKind.ConstructorInObject -> Errors.CONSTRUCTOR_IN_OBJECT
            DiagnosticKind.DeserializationError -> FirErrors.DESERIALIZATION_ERROR
            DiagnosticKind.InferenceError -> FirErrors.INFERENCE_ERROR
            DiagnosticKind.NoSupertype -> FirErrors.NO_SUPERTYPE
            DiagnosticKind.TypeParameterAsSupertype -> FirErrors.TYPE_PARAMETER_AS_SUPERTYPE
            DiagnosticKind.EnumAsSupertype -> FirErrors.ENUM_AS_SUPERTYPE
            DiagnosticKind.RecursionInSupertypes -> FirErrors.RECURSION_IN_SUPERTYPES
            DiagnosticKind.RecursionInImplicitTypes -> FirErrors.RECURSION_IN_IMPLICIT_TYPES
            DiagnosticKind.Java -> FirErrors.ERROR_FROM_JAVA_RESOLUTION
            DiagnosticKind.Other -> FirErrors.OTHER_ERROR
        } as DiagnosticFactory0<PsiElement>
    }
}