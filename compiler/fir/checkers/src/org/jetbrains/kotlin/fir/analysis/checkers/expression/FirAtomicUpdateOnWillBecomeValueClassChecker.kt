/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.reportIdentitySensitiveOperationOnWillBecomeValueClass
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * The atomic update functions compare the current value with the one they have read by identity, which a value class does not have.
 */
object FirAtomicUpdateOnWillBecomeValueClassChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val updateCallableIds = listOf("update", "fetchAndUpdate", "updateAndFetch", "updateAt", "fetchAndUpdateAt", "updateAndFetchAt")
        .mapTo(mutableSetOf()) { CallableId(FqName("kotlin.concurrent.atomics"), Name.identifier(it)) }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (expression.toResolvedCallableSymbol()?.callableId !in updateCallableIds) return
        val valueType = (expression.typeArguments.firstOrNull() as? FirTypeProjectionWithVariance)?.typeRef?.coneType ?: return
        reportIdentitySensitiveOperationOnWillBecomeValueClass(expression.calleeReference.source, valueType)
    }
}
