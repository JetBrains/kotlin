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
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirDelegatedPropertyChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val delegate = declaration.delegate ?: return
        val delegateType = delegate.typeRef.coneType

        // TODO: Also suppress delegate issue if type inference failed. For example, in
        //  compiler/testData/diagnostics/tests/delegatedProperty/inference/differentDelegatedExpressions.fir.kt, no delegate issues are
        //  reported due to the inference issue.
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
                val hasReferenceError = hasFunctionReferenceErrors(functionCall)
                if (isGet && !hasReferenceError) checkReturnType(functionCall)
            }

            private fun hasFunctionReferenceErrors(functionCall: FirFunctionCall): Boolean {
                val errorNamedReference = functionCall.calleeReference as? FirErrorNamedReference ?: return false
                if (errorNamedReference.source?.kind != FirFakeSourceElementKind.DelegatedPropertyAccessor) return false
                val expectedFunctionSignature =
                    (if (isGet) "getValue" else "setValue") + "(${functionCall.arguments.joinToString(", ") { it.typeRef.coneType.render() }})"
                val delegateDescription = if (isGet) "delegate" else "delegate for var (read-write property)"

                fun reportInapplicableDiagnostics(candidates: Collection<FirBasedSymbol<*>>) {
                    reporter.reportOn(
                        errorNamedReference.source,
                        FirErrors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE,
                        expectedFunctionSignature,
                        candidates,
                        context
                    )
                }

                return when (val diagnostic = errorNamedReference.diagnostic) {
                    is ConeUnresolvedNameError -> {
                        reporter.reportOn(
                            errorNamedReference.source,
                            FirErrors.DELEGATE_SPECIAL_FUNCTION_MISSING,
                            expectedFunctionSignature,
                            delegateType,
                            delegateDescription,
                            context
                        )
                        true
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
                            reportInapplicableDiagnostics(diagnostic.candidates.map { it.symbol })
                        }
                        true
                    }
                    is ConeInapplicableWrongReceiver -> {
                        reporter.reportOn(
                            errorNamedReference.source,
                            FirErrors.DELEGATE_SPECIAL_FUNCTION_MISSING,
                            expectedFunctionSignature,
                            delegateType,
                            delegateDescription,
                            context
                        )
                        true
                    }
                    is ConeInapplicableCandidateError -> {
                        reportInapplicableDiagnostics(listOf(diagnostic.candidate.symbol))
                        true
                    }
                    else -> false
                }
            }

            private fun checkReturnType(functionCall: FirFunctionCall) {
                val returnType = functionCall.typeRef.coneType
                val propertyType = declaration.returnTypeRef.coneType
                if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, returnType, propertyType)) {
                    reporter.reportOn(
                        delegate.source,
                        FirErrors.DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH,
                        "getValue",
                        propertyType,
                        returnType,
                        context
                    )
                }
            }
        }

        declaration.getter?.body?.acceptChildren(DelegatedPropertyAccessorVisitor(true))
        declaration.setter?.body?.acceptChildren(DelegatedPropertyAccessorVisitor(false))
    }
}
