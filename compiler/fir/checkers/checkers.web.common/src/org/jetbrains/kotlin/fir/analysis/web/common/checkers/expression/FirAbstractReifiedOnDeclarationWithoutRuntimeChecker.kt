/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.FirAbstractWebCheckerUtils
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.forAllReifiedTypeParameters
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirAbstractReifiedOnDeclarationWithoutRuntimeChecker(
    private val webCheckerUtils: FirAbstractWebCheckerUtils,
    private val diagnostic: KtDiagnosticFactory1<ConeKotlinType>,
) : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext)
    open fun isDeclarationWithoutRuntime(symbol: FirClassifierSymbol<*>): Boolean =
        webCheckerUtils.isNativeOrExternalInterface(symbol, context.session)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        expression.forAllReifiedTypeParameters { type, typeArgument ->
            val typeSymbol = type.toSymbol()
            if (typeSymbol != null && isDeclarationWithoutRuntime(typeSymbol)) {
                reporter.reportOn(
                    typeArgument.source ?: expression.source,
                    diagnostic,
                    type
                )
            }
        }
    }
}
