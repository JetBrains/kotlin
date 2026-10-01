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
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object FirNativeIdentityHashCodeCallOnValueTypeObjectChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val identityHashCodeCallableId = CallableId(FqName("kotlin.native"), Name.identifier("identityHashCode"),)

    private val operationsToCheckFirstArgCallableIds = setOf(
        CallableId(FqName("kotlin.native.ref"), FqName("WeakReference"), Name.identifier("WeakReference")),
        CallableId(FqName("kotlin.native.ref"), Name.identifier("createCleaner")),
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.toResolvedCallableSymbol() ?: return
        when (symbol.callableId) {
            identityHashCodeCallableId -> {
                val argumentType = expression.extensionReceiver?.resolvedType ?: return
                if (argumentType.isValueType()) {
                    reporter.reportOn(expression.source, FirNativeErrors.IDENTITY_HASH_CODE_ON_VALUE_TYPE, argumentType)
                }
            }
            in operationsToCheckFirstArgCallableIds -> {
                val resourceParameter = (symbol as? FirFunctionSymbol<*>)?.valueParameterSymbols?.firstOrNull() ?: return
                val argument = expression.resolvedArgumentMapping?.entries?.firstOrNull { it.value.symbol == resourceParameter }?.key ?: return
                val argumentType = argument.resolvedType
                if (argumentType.isValueType()) {
                    reporter.reportOn(argument.source, FirNativeErrors.IDENTITY_SENSITIVE_OPERATION_ON_VALUE_TYPE, argumentType)
                }
            }
        }
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.isValueType(): Boolean = isPrimitiveOrNullablePrimitive || isValueClass(context.session)
}