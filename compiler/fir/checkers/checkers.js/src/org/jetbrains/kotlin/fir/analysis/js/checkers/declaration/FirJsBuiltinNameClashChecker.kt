/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.getStableNameInJavaScript
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName

object FirJsBuiltinNameClashChecker : FirBasicDeclarationChecker() {
    private val PROHIBITED_STATIC_NAMES = setOf("prototype", "length", "\$metadata\$")
    private val PROHIBITED_MEMBER_NAMES = setOf("constructor")

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.symbol.isNativeObject(context.session)) {
            return
        }
        if (declaration.getContainingClassSymbol(context.session) == null) {
            return
        }

        val stableName = declaration.symbol.getStableNameInJavaScript(context.session) ?: return

        if (declaration is FirClassLikeDeclaration && stableName in PROHIBITED_STATIC_NAMES) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_BUILTIN_NAME_CLASH, "Function.$stableName", context)
        }
        if (declaration is FirCallableDeclaration && stableName in PROHIBITED_MEMBER_NAMES) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_BUILTIN_NAME_CLASH, "Object.prototype.$stableName", context)
        }
    }
}
