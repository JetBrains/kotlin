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
import org.jetbrains.kotlin.fir.analysis.js.checkers.checkJsModuleUsage
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.superClassNotAny
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNonModule

object FirJsModuleChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        checkSuperClass(declaration, context, reporter)

        if (declaration is FirFile || !declaration.isEitherModuleOrNonModule(context.session)) return

        if (declaration is FirProperty && declaration.isVar) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR, context)
        }

        val closestNonLocal = context.closestNonLocalWith(declaration)?.symbol ?: return

        if (!closestNonLocal.isNativeObject(context)) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE, context)
        }

        if (context.isTopLevel && context.containingFile?.isEitherModuleOrNonModule(context.session) == true) {
            reporter.reportOn(declaration.source, FirJsErrors.NESTED_JS_MODULE_PROHIBITED, context)
        }
    }

    private fun checkSuperClass(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val classDeclaration = declaration as? FirClass ?: return
        val superClassSymbol = classDeclaration.superClassNotAny(context.session)?.toSymbol(context.session) ?: return

        val superClassRef = classDeclaration.superTypeRefs.firstOrNull {
            it.coneTypeOrNull?.toSymbol(context.session) == superClassSymbol
        }
        checkJsModuleUsage(superClassSymbol, context, reporter, superClassRef?.source ?: declaration.source)
    }

    private fun FirDeclaration.isEitherModuleOrNonModule(session: FirSession) =
        hasAnnotation(JsModule, session) || hasAnnotation(JsNonModule, session)
}
