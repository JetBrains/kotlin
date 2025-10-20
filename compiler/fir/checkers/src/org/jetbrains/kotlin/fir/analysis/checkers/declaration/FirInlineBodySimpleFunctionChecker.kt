/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

object FirInlineBodySimpleFunctionChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        if (isInsideInlineContext(declaration)) {
            reporter.reportOn(declaration.source, FirErrors.NOT_YET_SUPPORTED_IN_INLINE, "Local functions")
        }
    }

    context(context: CheckerContext)
    fun isInsideInlineContext(declaration: FirDeclaration): Boolean {
        var outerInlineContext = when {
            declaration == context.inlineFunctionBodyContext?.inlineFunction -> context.inlineFunctionBodyContext?.parentInlineContext
            else -> context.inlineFunctionBodyContext
        }
        for (it in context.containingDeclarations.asReversed()) {
            when {
                it == outerInlineContext?.inlineFunction?.symbol -> when {
                    !outerInlineContext.inlineFunction.symbol.isLocal -> return true
                    else -> outerInlineContext = outerInlineContext.parentInlineContext
                }
                it.isObject -> return false
            }
        }

        return false
    }

    private val FirBasedSymbol<*>.isObject: Boolean
        get() = this is FirAnonymousObjectSymbol || this is FirRegularClassSymbol && classKind.isObject
}
