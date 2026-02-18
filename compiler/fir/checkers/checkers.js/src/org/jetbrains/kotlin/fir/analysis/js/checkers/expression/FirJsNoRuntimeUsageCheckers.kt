/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirTypeOperatorCallChecker
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirGetClassCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.JsStandardClassIds
import kotlin.collections.orEmpty
import kotlin.collections.plus

object FirJsNoRuntimeTypeOperatorChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        val op = expression.operation
        if (op != FirOperation.IS && op != FirOperation.NOT_IS && op != FirOperation.AS && op != FirOperation.SAFE_AS) return

        val rhsType = (expression.conversionTypeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType) ?: return
        val classSymbol = rhsType.toRegularClassSymbol() ?: return
        if (!classSymbol.isInterface) return


        if (classSymbol.hasAnnotation(JsStandardClassIds.Annotations.JsNoRuntime, context.session)) {
            when (op) {
                FirOperation.IS, FirOperation.NOT_IS -> reporter.reportOn(expression.source, FirJsErrors.JS_NO_RUNTIME_FORBIDDEN_IS_CHECK)
                FirOperation.AS, FirOperation.SAFE_AS -> reporter.reportOn(expression.source, FirJsErrors.JS_NO_RUNTIME_FORBIDDEN_AS_CAST)
            }
        }
    }
}

object FirJsNoRuntimeClassReferenceChecker : FirGetClassCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirGetClassCall) {
        val classSymbol = expression.argument.resolvedType.toRegularClassSymbol()?.takeIf { it.isInterface } ?: return

        if (classSymbol.hasAnnotation(JsStandardClassIds.Annotations.JsNoRuntime, context.session)) {
            reporter.reportOn(expression.source, FirJsErrors.JS_NO_RUNTIME_FORBIDDEN_CLASS_REFERENCE)
        }
    }
}
