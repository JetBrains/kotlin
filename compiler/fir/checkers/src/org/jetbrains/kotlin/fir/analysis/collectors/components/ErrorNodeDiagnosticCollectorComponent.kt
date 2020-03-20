/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.onSource
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.diagnostics.*
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

    private fun reportFirDiagnostic(diagnostic: FirDiagnostic, source: FirSourceElement, reporter: DiagnosticReporter) {
        val coneDiagnostic = when (diagnostic) {
            is FirUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.onSource(source, diagnostic.name?.asString())
            is FirUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.onSource(source, diagnostic.classId.asString())
            is FirUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.onSource(source, diagnostic.name.asString())
            is FirInapplicableCandidateError -> FirErrors.INAPPLICABLE_CANDIDATE.onSource(source, diagnostic.candidates.map { it.symbol })
            is FirAmbiguityError -> FirErrors.AMBIGUITY.onSource(source, diagnostic.candidates)
            is FirOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.onSource(source, diagnostic.candidates)
            is FirVariableExpectedError -> Errors.VARIABLE_EXPECTED.onSource(source)
            is FirTypeMismatchError -> FirErrors.TYPE_MISMATCH.onSource(source, diagnostic.expectedType, diagnostic.actualType)
            is FirSimpleDiagnostic -> diagnostic.getFactory().onSource(source)
            is FirDiagnosticWithParameters1<*> -> diagnostic.getFactory().tryOnSource(source, diagnostic.a)
            is FirStubDiagnostic -> null
            else -> throw IllegalArgumentException("Unsupported diagnostic type: ${diagnostic.javaClass}")
        }
        reporter.report(coneDiagnostic)
    }

    private fun FirSimpleDiagnostic.getFactory(): DiagnosticFactory0<PsiElement> {
        @Suppress("UNCHECKED_CAST")
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
            else -> throw IllegalArgumentException("Unsupported diagnostic kind: $kind at $javaClass")
        } as DiagnosticFactory0<PsiElement>
    }

    private fun FirDiagnosticWithParameters1<*>.getFactory(): DiagnosticFactory1<PsiElement, Any?> {
        @Suppress("UNCHECKED_CAST")
        return when (kind) {
            DiagnosticKind.SuperNotAllowed -> Errors.SUPER_IS_NOT_AN_EXPRESSION
            else -> throw IllegalArgumentException("Unsupported diagnostic kind: $kind at $javaClass")
        } as DiagnosticFactory1<PsiElement, Any?>
    }

    private inline fun <reified E : PsiElement, reified A> DiagnosticFactory1<E, A>.tryOnSource(
        source: FirSourceElement,
        a: Any?
    ): ConeDiagnostic? {
        val aa = a as? A ?: throw IllegalArgumentException("Parameter passed to the factory is of incompatible type!")
        return onSource(source, aa)
    }
}