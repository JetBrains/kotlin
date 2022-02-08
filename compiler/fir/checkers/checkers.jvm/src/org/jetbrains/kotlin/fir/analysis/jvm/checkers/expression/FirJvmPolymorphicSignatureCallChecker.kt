/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.name.ClassId

object FirJvmPolymorphicSignatureCallChecker : FirFunctionCallChecker() {
    private val polymorphicSignatureClassId = ClassId.fromString("java/lang/invoke/MethodHandle.PolymorphicSignature")

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.PolymorphicSignature)) return
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (callableSymbol.getAnnotationByClassId(polymorphicSignatureClassId) == null) return

        for (valueArgument in expression.arguments) {
            if (valueArgument is FirVarargArgumentsExpression) {
                for (argument in valueArgument.arguments) {
                    if (argument is FirSpreadArgumentExpression) {
                        reporter.reportOn(argument.source, FirJvmErrors.SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL, context)
                    }
                }
            }
        }
    }
}