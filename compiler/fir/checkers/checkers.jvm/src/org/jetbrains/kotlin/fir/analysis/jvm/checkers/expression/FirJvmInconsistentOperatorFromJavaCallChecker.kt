/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirJvmInconsistentOperatorFromJavaCallChecker : FirFunctionCallChecker() {
    private val CONCURRENT_HASH_MAP_CALLABLE_ID = CallableId(
        ClassId.fromString("java/util/concurrent/ConcurrentHashMap"),
        Name.identifier("contains")
    )

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        if (callableSymbol.name != OperatorNameConventions.CONTAINS) return
        val valueParameterSymbol = callableSymbol.valueParameterSymbols.singleOrNull() ?: return
        val type = valueParameterSymbol.resolvedReturnTypeRef.coneType.lowerBoundIfFlexible()
        if (!type.isAny && !type.isNullableAny) return

        if (expression.origin != FirFunctionCallOrigin.Operator || expression.origin.ordinal != 2) return

        callableSymbol.check(expression.calleeReference.source, context, reporter)
    }

    fun FirNamedFunctionSymbol.check(source: KtSourceElement?, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        if (callableId == CONCURRENT_HASH_MAP_CALLABLE_ID) {
            reporter.reportOn(source, FirJvmErrors.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR, context)
            return true
        }

        val containingClass = containingClassLookupTag()?.toFirRegularClassSymbol(context.session) ?: return false
        val overriddenFunctions = overriddenFunctions(containingClass, context)
        for (overriddenFunction in overriddenFunctions) {
            if (overriddenFunction is FirNamedFunctionSymbol && overriddenFunction.check(source, context, reporter)) {
                return true
            }
        }

        return false
    }
}
