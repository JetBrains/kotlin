/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.hasExplicitReturnType
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionTypeConversionExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSuspendOrKSuspendFunctionType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext

/**
 * true if this type is a suspend function type (`suspend () -> Unit`, `KSuspendFunction0`, etc.) or a subtype of one,
 * i.e. a class/object/interface that implements such a type as a supertype.
 */
internal fun ConeKotlinType.isSuspendFunctionTypeOrSubtype(session: FirSession): Boolean {
    return with(session.typeContext) { isTypeOrSubtypeOf { it.isSuspendOrKSuspendFunctionType(session) } }
}

/**
 * true if this expression is a value directly constructed right here: a constructor call, an object literal, or a
 * reference to a named `object`. A `val` with no explicit type is unwrapped to its initializer, since its inferred
 * type can't hide the real shape. Anything else reached indirectly (parameter, `var`, explicitly-typed property)
 * is treated as safe, since telling it apart from a real callable would need data-flow analysis.
 */
context(context: CheckerContext)
internal fun FirExpression.isDirectSuspendFunctionalObjectConstruction(): Boolean {
    return when (val unwrapped = unwrapArgument()) {
        is FirAnonymousObjectExpression -> true
        is FirResolvedQualifier -> unwrapped.isNamedObjectReference()
        is FirFunctionCall -> unwrapped.isConstructorCall()
        is FirFunctionTypeConversionExpression -> unwrapped.expression.isDirectSuspendFunctionalObjectConstruction()
        is FirPropertyAccessExpression -> unwrapped.unwrapImplicitlyTypedValInitializer()
            ?.isDirectSuspendFunctionalObjectConstruction() ?: false
        else -> false
    }
}

private fun FirResolvedQualifier.isNamedObjectReference(): Boolean =
    accessedObjectSymbol != null

private fun FirFunctionCall.isConstructorCall(): Boolean =
    calleeReference.toResolvedCallableSymbol() is FirConstructorSymbol

/** Returns the initializer of the accessed `val`, but only when it has no explicit type (so nothing is hidden). */
private fun FirPropertyAccessExpression.unwrapImplicitlyTypedValInitializer(): FirExpression? {
    val property = calleeReference.toResolvedVariableSymbol() as? FirPropertySymbol ?: return null
    if (property.isVar || property.hasExplicitReturnType) return null
    return property.resolvedInitializer
}

/**
 * Reports [FirJsErrors.JS_SUSPEND_FUNCTION_INTERFACE_CAST] on [reportSource] (or [rawExpression]'s source) if
 * [rawExpression] is a direct construction of a suspend-functional object whose type is (a subtype of) [forcedType]
 * (or its own resolved type).
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun reportIfUnsafeSuspendFunctionalValue(
    rawExpression: FirExpression,
    forcedType: ConeKotlinType? = null,
    reportSource: KtSourceElement? = null,
) {
    val expression = rawExpression.unwrapArgument()
    val type = forcedType ?: expression.resolvedType
    if (!type.isSuspendFunctionTypeOrSubtype(context.session)) return
    if (!expression.isDirectSuspendFunctionalObjectConstruction()) return
    reporter.reportOn(reportSource ?: rawExpression.source, FirJsErrors.JS_SUSPEND_FUNCTION_INTERFACE_CAST)
}
