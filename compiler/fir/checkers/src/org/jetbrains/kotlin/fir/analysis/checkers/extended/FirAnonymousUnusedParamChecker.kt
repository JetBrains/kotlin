/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirAnonymousFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNUSED_ANONYMOUS_PARAMETER
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirAnonymousUnusedParamChecker : FirAnonymousFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirAnonymousFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        context.lambdaBodyContext?.checkUnusedParams(declaration, context, reporter)
    }

    class LambdaBodyContext(private val outermostLambda: FirAnonymousFunction) {
        internal fun checkUnusedParams(declaration: FirAnonymousFunction, context: CheckerContext, reporter: DiagnosticReporter) {
            // We need to check only outermost Lambda which will detect unused params of nested Lambdas too.
            if (declaration != outermostLambda)
                return

            val unusedParams = declaration.valueParameters.map { it.symbol }.filter { it.source !is KtFakeSourceElement }.toMutableSet()
            declaration.body?.accept(unusedParamsVisitor, unusedParams)

            unusedParams.forEach {
                reporter.reportOn(it.source, UNUSED_ANONYMOUS_PARAMETER, it, context)
            }
        }
    }

    private val unusedParamsVisitor: FirVisitor<Unit, MutableSet<FirValueParameterSymbol>> =
        object : FirVisitor<Unit, MutableSet<FirValueParameterSymbol>>() {
            override fun visitElement(element: FirElement, data: MutableSet<FirValueParameterSymbol>) {
                if (data.isNotEmpty()) {
                    element.acceptChildren(this, data)
                }
            }

            override fun visitResolvedNamedReference(
                resolvedNamedReference: FirResolvedNamedReference,
                data: MutableSet<FirValueParameterSymbol>,
            ) {
                data.removeAll { resolvedNamedReference.resolvedSymbol == it }
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: MutableSet<FirValueParameterSymbol>) {
                if (!anonymousFunction.isLambda) {
                    this.visitElement(anonymousFunction, data)
                    return
                }

                val unusedParams =
                    anonymousFunction.valueParameters.map { it.symbol }.filter { it.source !is KtFakeSourceElement }.toMutableSet()
                data.addAll(unusedParams)

                anonymousFunction.acceptChildren(this, data)
            }
        }
}

fun createLambdaBodyContext(lambda: FirAnonymousFunction, context: CheckerContext): FirAnonymousUnusedParamChecker.LambdaBodyContext {
    return context.lambdaBodyContext ?: FirAnonymousUnusedParamChecker.LambdaBodyContext(lambda)
}