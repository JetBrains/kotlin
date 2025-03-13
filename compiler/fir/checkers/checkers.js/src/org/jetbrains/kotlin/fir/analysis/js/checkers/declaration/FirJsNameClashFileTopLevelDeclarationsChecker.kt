/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFileChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsStableName
import org.jetbrains.kotlin.fir.analysis.js.checkers.collectNameClashesWith
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

object FirJsNameClashFileTopLevelDeclarationsChecker : FirFileChecker(MppCheckerKind.Common) {
    private fun MutableMap<String, MutableList<FirJsStableName>>.addStableName(
        symbol: FirBasedSymbol<*>,
        context: CheckerContext
    ) {
        val stableName = FirJsStableName.createStableNameOrNull(symbol, context.session)
        if (stableName != null) {
            getOrPut(stableName.name) { mutableListOf() }.add(stableName)
        }
        if (symbol is FirPropertySymbol) {
            symbol.getterSymbol?.let { getter -> addStableName(getter, context) }
            symbol.setterSymbol?.let { setter -> addStableName(setter, context) }
        }
    }

    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        val topLevelDeclarationsWithStableName = mutableMapOf<String, MutableList<FirJsStableName>>()
        @OptIn(DirectDeclarationsAccess::class)
        for (topLevelDeclaration in declaration.declarations) {
            topLevelDeclarationsWithStableName.addStableName(topLevelDeclaration.symbol, context)
        }
        for ((name, stableNames) in topLevelDeclarationsWithStableName.entries) {
            for (symbol in stableNames) {
                val clashed = stableNames.collectNameClashesWith(symbol).takeIf { it.isNotEmpty() } ?: continue
                val source = symbol.symbol.source ?: declaration.source
                reporter.reportOn(source, FirJsErrors.JS_NAME_CLASH, name, clashed.map { it.symbol }, context)
            }
        }
    }
}
