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
import org.jetbrains.kotlin.fir.resolve.calls.UnsafeCall
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirForLoopChecker : FirBlockChecker(MppCheckerKind.Common) {
    override fun check(expression: FirBlock, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.source?.kind != KtFakeSourceElementKind.DesugaredForLoop) return

        val statements = expression.statements
        val iteratorDeclaration = statements[0] as? FirProperty ?: return
        val whileLoop = statements[1] as? FirWhileLoop ?: return
        if (iteratorDeclaration.source?.kind != KtFakeSourceElementKind.DesugaredForLoop) return
        val iteratorCall = iteratorDeclaration.initializer as FirFunctionCall
        val source = iteratorCall.explicitReceiver?.source ?: iteratorCall.source
        if (checkSpecialFunctionCall(
                iteratorCall,
                reporter,
                source,
                context,
                ITERATOR_AMBIGUITY,
                ITERATOR_MISSING,
                unsafeCallFactory = ITERATOR_ON_NULLABLE
            )
        ) {
            return
        }

        val hasNextCall = whileLoop.condition as FirFunctionCall
        checkSpecialFunctionCall(
            hasNextCall,
            reporter,
            source,
            context,
            HAS_NEXT_FUNCTION_AMBIGUITY,
            HAS_NEXT_MISSING,
            noneApplicableFactory = HAS_NEXT_FUNCTION_NONE_APPLICABLE
        )

        val loopParameter = whileLoop.block.statements.firstOrNull() as? FirProperty ?: return
        if (loopParameter.initializer?.source?.kind != KtFakeSourceElementKind.DesugaredForLoop) return
        val nextCall = loopParameter.initializer as FirFunctionCall
        checkSpecialFunctionCall(
            nextCall,
            reporter,
            source,
            context,
            NEXT_AMBIGUITY,
            NEXT_MISSING,
            noneApplicableFactory = NEXT_NONE_APPLICABLE
        )

        val loopParameterSource = loopParameter.source
        loopParameterSource.valOrVarKeyword?.let {
            reporter.reportOn(loopParameterSource, FirErrors.VAL_OR_VAR_ON_LOOP_PARAMETER, it, context)
        }
    }

    private fun checkSpecialFunctionCall(
        call: FirFunctionCall,
        reporter: DiagnosticReporter,
        reportSource: KtSourceElement?,
        context: CheckerContext,
        ambiguityFactory: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>>,
        missingFactory: KtDiagnosticFactory0,
        noneApplicableFactory: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>>? = null,
        unsafeCallFactory: KtDiagnosticFactory0? = null,
    ): Boolean {
        val calleeReference = call.calleeReference
        when {
            calleeReference.isError() -> {
                when (val diagnostic = calleeReference.diagnostic) {
                    is ConeAmbiguityError -> {
                        reporter.reportOn(
                            reportSource,
                            noneApplicableFactory ?: ambiguityFactory,
                            diagnostic.candidates.map { it.symbol },
                            context
                        )
                    }
                    is ConeUnresolvedNameError -> {
                        reporter.reportOn(reportSource, missingFactory, context)
                    }
                    is ConeInapplicableWrongReceiver -> when {
                        noneApplicableFactory != null -> {
                            reporter.reportOn(reportSource, noneApplicableFactory, diagnostic.candidateSymbols, context)
                        }
                        calleeReference.name == OperatorNameConventions.ITERATOR -> {
                            reporter.reportOn(reportSource, missingFactory, context)
                        }
                        else -> {
                            error("ConeInapplicableWrongReceiver, but no diagnostic reported")
                        }
                    }
                    is ConeConstraintSystemHasContradiction -> {
                        if (calleeReference.name == OperatorNameConventions.ITERATOR)
                            reporter.reportOn(reportSource, missingFactory, context)
                    }
                    is ConeInapplicableCandidateError -> {
                        if (unsafeCallFactory != null || noneApplicableFactory != null) {
                            diagnostic.candidate.diagnostics.filter { it.applicability == diagnostic.applicability }.forEach {
                                when (it) {
                                    is UnsafeCall -> {
                                        if (unsafeCallFactory != null) {
                                            reporter.reportOn(
                                                reportSource, unsafeCallFactory, context
                                            )
                                        } else {
                                            reporter.reportOn(
                                                reportSource, noneApplicableFactory!!, listOf(diagnostic.candidate.symbol), context
                                            )
                                        }
                                        return true
                                    }
                                    is OperatorCallOfNonOperatorFunction -> {
                                        val symbol = it.function
                                        reporter.reportOn(
                                            reportSource, OPERATOR_MODIFIER_REQUIRED, symbol, symbol.name.asString(), context
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
                        reporter.reportOn(reportSource, OPERATOR_MODIFIER_REQUIRED, symbol, symbol.name.asString(), context)
                        // Don't return true as we want to report other errors
                    }
                }
            }
        }
        return false
    }
}
