/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
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
        val type = expression.typeArguments[0].toConeTypeProjection()
        if (type !is ConeClassLikeType || type.typeArguments.isNotEmpty() || !type.isInlineClass(context.session)) {
            if (expression.typeArguments[0].source == null) {
                reporter.reportOn(expression.source, FirErrors.INTRINSIC_BOXING_CALL_BAD_INFERRED_TYPE_ARGUMENT, context)
            } else {
                reporter.reportOn(expression.typeArguments[0].source, FirErrors.INTRINSIC_BOXING_CALL_ILLEGAL_TYPE_ARGUMENT, context)
            }
            return
        }
        if (expression.argumentList.arguments.size != 1) return
        val argumentType = expression.argument.typeRef.coneType
        val inlineClassUnderlyingType = type.toRegularClassSymbol(context.session)
            ?.primaryConstructorSymbol()?.valueParameterSymbols?.singleOrNull()?.resolvedReturnType ?: return
        if (!argumentType.isSubtypeOf(inlineClassUnderlyingType, context.session)) {
            reporter.reportOn(
                expression.argument.source,
                FirErrors.INTRINSIC_BOXING_CALL_ARGUMENT_TYPE_MISMATCH,
                argumentType,
                type,
                inlineClassUnderlyingType,
                context
            )
        }
    }

}