/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsWebCheckerUtils
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirAbstractReifiedOnDeclarationWithoutRuntimeChecker
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds

@OptIn(SymbolInternals::class)
object FirJsReifiedJsNoRuntimeChecker : FirAbstractReifiedOnDeclarationWithoutRuntimeChecker(
    webCheckerUtils = FirJsWebCheckerUtils,
    diagnostic = FirJsErrors.JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT
) {
    context(context: CheckerContext)
    override fun isDeclarationWithoutRuntime(symbol: FirClassifierSymbol<*>): Boolean =
        (symbol.fir as? FirClass)?.let {
            it.isInterface && it.hasAnnotation(JsStandardClassIds.Annotations.JsNoRuntime, context.session)
        } == true
}
