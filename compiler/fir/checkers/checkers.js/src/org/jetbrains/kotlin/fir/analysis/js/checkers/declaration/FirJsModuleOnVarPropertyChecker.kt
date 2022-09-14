/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNonModule

object FirJsModuleOnVarPropertyChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isVar) {
            return
        }

        for (annotation in declaration.annotations) {
            val call = annotation as? FirAnnotationCall ?: continue
            val annotationFqName = (call.calleeReference.resolvedSymbol as? FirCallableSymbol<*>)?.callableId?.asSingleFqName()?.parent()

            if (annotationFqName == JsModule.asSingleFqName() || annotationFqName == JsNonModule.asSingleFqName()) {
                reporter.reportOn(annotation.source, FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR, context)
            }
        }
    }
}
