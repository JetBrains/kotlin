/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.getExplicitAnnotationRetention

object FirJsRuntimeAnnotationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        for (annotation in declaration.annotations) {
            val annotationClassSymbol = annotation.toAnnotationClassLikeSymbol(context.session) ?: continue
            if (annotationClassSymbol.getExplicitAnnotationRetention(context.session) != AnnotationRetention.RUNTIME) continue

            if (declaration is FirMemberDeclaration && declaration.symbol.isEffectivelyExternal(context)) {
                reporter.reportOn(annotation.source, FirJsErrors.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION, context)
            } else {
                reporter.reportOn(annotation.source, FirJsErrors.RUNTIME_ANNOTATION_NOT_SUPPORTED, context)
            }
        }
    }
}