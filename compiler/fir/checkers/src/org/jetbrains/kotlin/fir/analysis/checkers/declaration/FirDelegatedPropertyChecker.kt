/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess

object FirDelegatedPropertyChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val delegate = declaration.delegate ?: return
        val delegateType = delegate.typeRef.coneType

        if (delegateType is ConeKotlinErrorType) {
            val delegateSource = delegate.source
            // Implicit recursion type is not reported since the type ref does not have a real source.
            if (delegateSource != null && (delegateType.diagnostic as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.RecursionInImplicitTypes) {
                // skip reporting other issues in this case
                reporter.reportOn(delegateSource, FirErrors.RECURSION_IN_IMPLICIT_TYPES, context)
            }
            return
        }

        class DelegatedPropertyAccessorVisitor(private val isGet: Boolean) : FirVisitorVoid() {
            override fun visitElement(element: FirElement) = element.acceptChildren(this)

            override fun visitFunctionCall(functionCall: FirFunctionCall) {
                val errorNamedReference = functionCall.calleeReference as? FirErrorNamedReference ?: return
                if (errorNamedReference.source?.kind != FirFakeSourceElementKind.DelegatedPropertyAccessor) return
                val expectedFunctionSignature =
                    (if (isGet) "getValue" else "setValue") + "(${functionCall.arguments.joinToString(", ") { it.typeRef.coneType.render() }})"
                val delegateDescription = if (isGet) "delegate" else "delegate for var (read-write property)"

                fun reportInapplicableDiagnostics(
                    candidateApplicability: CandidateApplicability,
                    candidates: Collection<AbstractFirBasedSymbol<*>>
                ) {
                    if (candidateApplicability == CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER) {
                        reporter.reportOn(
                            errorNamedReference.source,
                            FirErrors.DELEGATE_SPECIAL_FUNCTION_MISSING,
                            expectedFunctionSignature,
                            delegateType,
                            delegateDescription,
                            context
                        )
                    } else {
                        reporter.reportOn(
                            errorNamedReference.source,
                            FirErrors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE,
                            expectedFunctionSignature,
                            candidates,
                            context
                        )
                    }
                }

                when (val diagnostic = errorNamedReference.diagnostic) {
                    is ConeUnresolvedNameError -> {
                        reporter.reportOn(
                            errorNamedReference.source,
                            FirErrors.DELEGATE_SPECIAL_FUNCTION_MISSING,
                            expectedFunctionSignature,
                            delegateType,
                            delegateDescription,
                            context
                        )
                    }
                    is ConeAmbiguityError -> {
                        if (diagnostic.applicability.isSuccess) {
                            // Match is successful but there are too many matches! So we report DELEGATE_SPECIAL_FUNCTION_AMBIGUITY.
                            reporter.reportOn(
                                errorNamedReference.source,
                                FirErrors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY,
                                expectedFunctionSignature,
                                diagnostic.candidates.map { it.symbol },
                                context
                            )
                        } else {
                            reportInapplicableDiagnostics(diagnostic.applicability, diagnostic.candidates.map { it.symbol })
                        }
                    }
                    is ConeInapplicableCandidateError -> {
                        reportInapplicableDiagnostics(diagnostic.applicability, listOf(diagnostic.candidate.symbol))
                    }
                }
            }
        }

        declaration.getter?.body?.acceptChildren(DelegatedPropertyAccessorVisitor(true))
        declaration.setter?.body?.acceptChildren(DelegatedPropertyAccessorVisitor(false))
    }
}