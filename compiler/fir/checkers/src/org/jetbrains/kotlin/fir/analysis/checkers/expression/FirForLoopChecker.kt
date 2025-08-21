/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.valOrVarKeyword
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HAS_NEXT_FUNCTION_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HAS_NEXT_FUNCTION_NONE_APPLICABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HAS_NEXT_MISSING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ITERATOR_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ITERATOR_MISSING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ITERATOR_ON_NULLABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NEXT_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NEXT_MISSING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NEXT_NONE_APPLICABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OPERATOR_MODIFIER_REQUIRED
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.calls.OperatorCallOfNonOperatorFunction
import org.jetbrains.kotlin.fir.resolve.calls.InapplicableNullableReceiver
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.resolve.calls.tower.ApplicabilityDetail
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirForLoopChecker : FirBlockChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirBlock) {
        if (expression.source?.kind != KtFakeSourceElementKind.DesugaredForLoop) return

        val statements = expression.statements
        val iteratorDeclaration = statements[0] as? FirProperty ?: return
        val whileLoop = statements[1] as? FirWhileLoop ?: return
        if (iteratorDeclaration.source?.kind != KtFakeSourceElementKind.DesugaredForLoop) return
        val iteratorCall = iteratorDeclaration.initializer as FirFunctionCall
        val source = iteratorCall.explicitReceiver?.source ?: iteratorCall.source
        if (checkSpecialFunctionCall(
                iteratorCall,
                source,
                ambiguityFactory = ITERATOR_AMBIGUITY,
                missingFactory = ITERATOR_MISSING,
                nullableReceiverFactory = ITERATOR_ON_NULLABLE
            )
        ) {
            return
        }

        val hasNextCall = whileLoop.condition as FirFunctionCall
        checkSpecialFunctionCall(
            hasNextCall,
            source,
            ambiguityFactory = HAS_NEXT_FUNCTION_AMBIGUITY,
            missingFactory = HAS_NEXT_MISSING,
            noneApplicableFactory = HAS_NEXT_FUNCTION_NONE_APPLICABLE
        )

        val loopParameter = whileLoop.block.statements.firstOrNull() as? FirProperty ?: return
        if (loopParameter.initializer?.source?.kind != KtFakeSourceElementKind.DesugaredForLoop) return
        val nextCall = loopParameter.initializer as FirFunctionCall
        checkSpecialFunctionCall(
            nextCall,
            source,
            ambiguityFactory = NEXT_AMBIGUITY,
            missingFactory = NEXT_MISSING,
            noneApplicableFactory = NEXT_NONE_APPLICABLE
        )

        val loopParameterSource = loopParameter.source
        loopParameterSource.valOrVarKeyword?.let {
            reporter.reportOn(loopParameterSource, FirErrors.VAL_OR_VAR_ON_LOOP_PARAMETER, it)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkSpecialFunctionCall(
        call: FirFunctionCall,
        reportSource: KtSourceElement?,
        ambiguityFactory: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>>,
        missingFactory: KtDiagnosticFactory0,
        noneApplicableFactory: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>>? = null,
        nullableReceiverFactory: KtDiagnosticFactory0? = null,
    ): Boolean {
        val calleeReference = call.calleeReference
        when {
            calleeReference.isError() -> {
                @OptIn(ApplicabilityDetail::class)
                when (val diagnostic = calleeReference.diagnostic) {
                    is ConeAmbiguityError -> {
                        reporter.reportOn(
                            reportSource,
                            if (diagnostic.applicability.isSuccess || noneApplicableFactory == null) ambiguityFactory else noneApplicableFactory,
                            diagnostic.candidates.map { it.symbol })
                    }
                    is ConeUnresolvedNameError -> {
                        reporter.reportOn(reportSource, missingFactory)
                    }
                    is ConeInapplicableWrongReceiver -> when {
                        noneApplicableFactory != null -> {
                            reporter.reportOn(reportSource, noneApplicableFactory, diagnostic.candidateSymbols)
                        }
                        calleeReference.name == OperatorNameConventions.ITERATOR -> {
                            reporter.reportOn(reportSource, missingFactory)
                        }
                        else -> {
                            error("ConeInapplicableWrongReceiver, but no diagnostic reported")
                        }
                    }
                    is ConeConstraintSystemHasContradiction -> {
                        if (calleeReference.name == OperatorNameConventions.ITERATOR)
                            reporter.reportOn(reportSource, missingFactory)
                    }
                    is ConeInapplicableCandidateError -> {
                        if (nullableReceiverFactory != null || noneApplicableFactory != null) {
                            diagnostic.candidate.diagnostics.filter { it.applicability == diagnostic.applicability }.forEach {
                                when (it) {
                                    is InapplicableNullableReceiver -> {
                                        if (nullableReceiverFactory != null) {
                                            reporter.reportOn(
                                                reportSource, nullableReceiverFactory
                                            )
                                        } else {
                                            reporter.reportOn(
                                                reportSource, noneApplicableFactory!!, listOf(diagnostic.candidate.symbol)
                                            )
                                        }
                                        return true
                                    }
                                    is OperatorCallOfNonOperatorFunction -> {
                                        val symbol = it.function
                                        reporter.reportOn(
                                            reportSource, OPERATOR_MODIFIER_REQUIRED, symbol,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                return true
            }
            calleeReference is FirResolvedNamedReference -> {
                val symbol = calleeReference.resolvedSymbol
                if (symbol is FirNamedFunctionSymbol) {
                    if (!symbol.isOperator) {
                        reporter.reportOn(reportSource, OPERATOR_MODIFIER_REQUIRED, symbol)
                        // Don't return true as we want to report other errors
                    }
                }
            }
        }
        return false
    }
}
