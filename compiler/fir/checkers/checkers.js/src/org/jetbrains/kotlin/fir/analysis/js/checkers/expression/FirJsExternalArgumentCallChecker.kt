/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExternalArgument

object FirJsExternalArgumentCallChecker : FirCallChecker() {
    override fun check(expression: FirCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.resolvedArgumentMapping ?: return
        for ((argument, parameter) in arguments) {
            if (parameter.hasAnnotation(JsExternalArgument, context.session)) {
                val unwrappedArg = argument.unwrapArgument()
                val type = unwrappedArg.resolvedType
                val symbol = type.toRegularClassSymbol(context.session)
                if (symbol?.isEffectivelyExternal(context.session) == false || type is ConeDynamicType) {
                    reporter.reportOn(
                        unwrappedArg.source,
                        FirJsErrors.JS_EXTERNAL_ARGUMENT,
                        type,
                        context
                    )
                }
            }
        }
    }
}
