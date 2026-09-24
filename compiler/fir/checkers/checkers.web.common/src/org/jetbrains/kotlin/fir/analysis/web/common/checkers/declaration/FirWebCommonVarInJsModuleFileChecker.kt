/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * A top-level 'var' in a file annotated with a module annotation is compiled into a write to an imported
 * module binding, which is read-only in every supported module system.
 */
abstract class FirWebCommonVarInJsModuleFileChecker : FirPropertyChecker(MppCheckerKind.Common) {
    /**
     * The file-level annotations turning a file into a module declaration file.
     * JS has both 'JsModule' and 'JsNonModule', Wasm only has 'JsModule'.
     */
    abstract val moduleAnnotations: List<ClassId>

    abstract fun isExternalLike(symbol: FirBasedSymbol<*>, session: FirSession): Boolean

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!declaration.isVar || !context.isTopLevel) return
        if (context.containingFileSymbol?.hasModuleAnnotation(context.session) != true) return
        // An annotated 'var' is already covered by JS_MODULE_PROHIBITED_ON_VAR and NESTED_JS_MODULE_PROHIBITED.
        if (declaration.symbol.hasModuleAnnotation(context.session)) return
        // A non-external declaration is already covered by NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE
        // on JS and by JS_MODULE_PROHIBITED_ON_NON_EXTERNAL on Wasm.
        if (!isExternalLike(declaration.symbol, context.session)) return

        reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE)
    }

    private fun FirBasedSymbol<*>.hasModuleAnnotation(session: FirSession): Boolean {
        return moduleAnnotations.any { hasAnnotation(it, session) }
    }
}
