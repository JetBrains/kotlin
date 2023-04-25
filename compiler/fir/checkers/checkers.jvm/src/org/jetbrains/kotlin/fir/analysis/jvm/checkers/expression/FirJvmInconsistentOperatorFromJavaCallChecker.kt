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
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
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

/**
 * This checker detects if a call by operator 'contains' convention to a Java method violates the expected contract:
 * * "key in map" commonly resolves to stdlib extension that calls Map.containsKey(),
 * but there's a member in ConcurrentHashMap with acceptable signature that delegates to `containsValue` instead,
 * leading to an unexpected result. See KT-18053
 */
object FirJvmInconsistentOperatorFromJavaCallChecker : FirFunctionCallChecker() {
    private val CONCURRENT_HASH_MAP_CALLABLE_ID = CallableId(
        ClassId.fromString("java/util/concurrent/ConcurrentHashMap"),
        OperatorNameConventions.CONTAINS
    )

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        // Filter out non-operators
        if (expression.origin != FirFunctionCallOrigin.Operator) return
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        // Filter out non-contains
        if (callableSymbol.name != OperatorNameConventions.CONTAINS) return
        val valueParameterSymbol = callableSymbol.valueParameterSymbols.singleOrNull() ?: return
        val type = valueParameterSymbol.resolvedReturnTypeRef.coneType.lowerBoundIfFlexible()
        // Filter out handrolled contains with non-Any type
        if (!type.isAny && !type.isNullableAny) return
        callableSymbol.check(expression.calleeReference.source, context, reporter)
    }

    private fun FirNamedFunctionSymbol.check(source: KtSourceElement?, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        // Unwrap SubstitutionOverride origin if necessary
        if (originalOrSelf().callableId == CONCURRENT_HASH_MAP_CALLABLE_ID) {
            reporter.reportOn(source, FirJvmErrors.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR, context)
            return true
        }

        // Check explicitly overridden contains
        val containingClass = containingClassLookupTag()?.toFirRegularClassSymbol(context.session) ?: return false
        val overriddenFunctions = overriddenFunctions(containingClass, context, memberRequiredPhase = null)
        for (overriddenFunction in overriddenFunctions) {
            if (overriddenFunction is FirNamedFunctionSymbol && overriddenFunction.check(source, context, reporter)) {
                return true
            }
        }

        return false
    }
}
