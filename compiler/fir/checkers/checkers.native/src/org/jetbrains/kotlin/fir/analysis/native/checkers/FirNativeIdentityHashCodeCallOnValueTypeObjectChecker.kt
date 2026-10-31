/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.reportIdentitySensitiveOperation
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object FirNativeIdentityHashCodeCallOnValueTypeObjectChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val identityHashCodeCallableId = CallableId(FqName("kotlin.native"), Name.identifier("identityHashCode"),)

    private val weakReferenceConstructorCallableId =
        CallableId(FqName("kotlin.native.ref"), FqName("WeakReference"), Name.identifier("WeakReference"))

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.toResolvedCallableSymbol() ?: return
        when (symbol.callableId) {
            identityHashCodeCallableId -> {
                val argumentType = expression.extensionReceiver?.resolvedType ?: return
                reportIdentitySensitiveOperation(expression.source, argumentType) {
                    reporter.reportOn(expression.source, FirNativeErrors.IDENTITY_HASH_CODE_ON_VALUE_TYPE, argumentType)
                }
            }
            weakReferenceConstructorCallableId -> {
                val resourceParameter = (symbol as? FirFunctionSymbol<*>)?.valueParameterSymbols?.firstOrNull() ?: return
                val argument = expression.resolvedArgumentMapping?.entries?.firstOrNull { it.value.symbol == resourceParameter }?.key ?: return
                val argumentType = argument.resolvedType
                reportIdentitySensitiveOperation(argument.source, argumentType) {
                    reporter.reportOn(argument.source, FirErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, argumentType)
                }
            }
        }
    }
}