/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.closestNonLocalWith
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.checkJsModuleUsage
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.superClassNotAny
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNonModule

object FirJsModuleChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        checkSuperClass(declaration)

        if (declaration is FirFile || !declaration.isEitherModuleOrNonModule(context.session)) return

        if (declaration is FirProperty && declaration.isVar) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_VAR)
        }

        val closestNonLocal = context.closestNonLocalWith(declaration) ?: return

        if (!closestNonLocal.isNativeObject()) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE)
        }

        if (context.isTopLevel && context.containingFileSymbol?.isEitherModuleOrNonModule(context.session) == true) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.NESTED_JS_MODULE_PROHIBITED)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperClass(declaration: FirDeclaration) {
        val classDeclaration = declaration as? FirClass ?: return
        val superClassSymbol = classDeclaration.superClassNotAny(context.session)?.toSymbol() ?: return

        val superClassRef = classDeclaration.superTypeRefs.firstOrNull {
            it.coneTypeOrNull?.toSymbol() == superClassSymbol
        }
        checkJsModuleUsage(superClassSymbol, superClassRef?.source ?: declaration.source)
    }

    private fun FirDeclaration.isEitherModuleOrNonModule(session: FirSession): Boolean {
        return symbol.isEitherModuleOrNonModule(session)
    }

    private fun FirBasedSymbol<*>.isEitherModuleOrNonModule(session: FirSession): Boolean {
        return hasAnnotation(JsModule, session) || hasAnnotation(JsNonModule, session)
    }
}
