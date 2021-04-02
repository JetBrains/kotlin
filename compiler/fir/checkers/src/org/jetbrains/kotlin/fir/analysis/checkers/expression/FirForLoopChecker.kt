/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HAS_NEXT_FUNCTION_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HAS_NEXT_FUNCTION_NONE_APPLICABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HAS_NEXT_MISSING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ITERATOR_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ITERATOR_MISSING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ITERATOR_ON_NULLABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NEXT_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NEXT_MISSING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NEXT_NONE_APPLICABLE
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.InapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess

object FirForLoopChecker : FirBlockChecker() {
    override fun check(expression: FirBlock, context: CheckerContext, reporter: DiagnosticReporter) {
        val statements = expression.statements
        for ((iteratorDeclaration, whileLoop) in statements.windowed(2)) {
            if (iteratorDeclaration !is FirProperty) continue
            if (whileLoop !is FirWhileLoop) continue
            if (iteratorDeclaration.source?.kind != FirFakeSourceElementKind.DesugaredForLoop) continue
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
                continue
            }
            val hasNextCall = whileLoop.condition as FirFunctionCall
            if (checkSpecialFunctionCall(
                    hasNextCall,
                    reporter,
                    source,
                    context,
                    HAS_NEXT_FUNCTION_AMBIGUITY,
                    HAS_NEXT_MISSING,
                    noneApplicableFactory = HAS_NEXT_FUNCTION_NONE_APPLICABLE
                )
            ) {
                continue
            }
            val elementDeclaration = whileLoop.block.statements.firstOrNull() as? FirProperty ?: continue
            if (elementDeclaration.initializer?.source?.kind != FirFakeSourceElementKind.DesugaredForLoop) continue
            val nextCall = elementDeclaration.initializer as FirFunctionCall
            checkSpecialFunctionCall(
                nextCall,
                reporter,
                source,
                context,
                NEXT_AMBIGUITY,
                NEXT_MISSING,
                noneApplicableFactory = NEXT_NONE_APPLICABLE
            )
        }
    }

    private fun checkSpecialFunctionCall(
        call: FirFunctionCall,
        reporter: DiagnosticReporter,
        reportSource: FirSourceElement?,
        context: CheckerContext,
        ambiguityFactory: FirDiagnosticFactory1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>,
        missingFactory: FirDiagnosticFactory0<FirSourceElement, KtExpression>,
        noneApplicableFactory: FirDiagnosticFactory1<FirSourceElement, KtExpression, Collection<AbstractFirBasedSymbol<*>>>? = null,
        unsafeCallFactory: FirDiagnosticFactory0<FirSourceElement, KtExpression>? = null,
    ): Boolean {
        val calleeReference = call.calleeReference
        if (calleeReference is FirErrorNamedReference) {
            when (val diagnostic = calleeReference.diagnostic) {
                is ConeAmbiguityError -> if (diagnostic.applicability.isSuccess) {
                    reporter.reportOn(reportSource, ambiguityFactory, diagnostic.candidates, context)
                } else if (noneApplicableFactory != null) {
                    reporter.reportOn(reportSource, noneApplicableFactory, diagnostic.candidates, context)
                }
                is ConeUnresolvedNameError -> {
                    reporter.reportOn(reportSource, missingFactory, context)
                }
                is ConeInapplicableCandidateError -> {
                    if (unsafeCallFactory != null || noneApplicableFactory != null) {
                        diagnostic.candidate.diagnostics.filter { it.applicability == diagnostic.applicability }.forEach {
                            if (it is InapplicableWrongReceiver) {
                                if (unsafeCallFactory != null) {
                                    reporter.reportOn(reportSource, unsafeCallFactory, context)
                                } else {
                                    reporter.reportOn(reportSource, noneApplicableFactory!!, listOf(diagnostic.candidate.symbol), context)
                                }
                                return true
                            }
                        }
                    }
                }
            }
            return true
        }
        return false
    }
}