/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.closestNonLocalWith
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNonModule
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object FirJsModuleChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirFile || !declaration.isEitherModuleOrNonModule) return

        if (declaration is FirProperty && declaration.isVar) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR, context)
        }

        val closestNonLocal = context.closestNonLocalWith(declaration)?.symbol ?: return

        if (!closestNonLocal.isNativeObject(context)) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE, context)
        }

        if (context.isTopLevel) {
            val file = context.containingDeclarations.lastIsInstanceOrNull<FirFile>()

            if (file != null && file.isEitherModuleOrNonModule) {
                reporter.reportOn(declaration.source, FirJsErrors.NESTED_JS_MODULE_PROHIBITED, context)
            }
        }
    }

    private val FirDeclaration.isEitherModuleOrNonModule
        get() = annotations.any {
            val call = it as? FirAnnotationCall ?: return@any false
            val annotationFqName = (call.calleeReference.resolvedSymbol as? FirCallableSymbol<*>)?.callableId?.asSingleFqName()?.parent()
            annotationFqName == JsModule.asSingleFqName() || annotationFqName == JsNonModule.asSingleFqName()
        }
}
