/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.Name

object FirInlineClassIntrinsicCreatorChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.CustomBoxingInInlineClasses)) return
        val callable = expression.calleeReference.toResolvedCallableSymbol() ?: return
        val callableId = (callable as? FirNamedFunctionSymbol)?.callableId ?: return
        if (callableId.callableName != Name.identifier("createInlineClassInstance") || callableId.packageName != StandardNames.BUILT_INS_PACKAGE_FQ_NAME) {
            return
        }
        if (expression.typeArguments.size != 1) return
        val type = expression.typeArguments[0]
        println(type)
    }

}