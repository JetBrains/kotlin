/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.createConeSubstitutorFromTypeArguments
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeParameterSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.types.arrayElementTypeArgument
import org.jetbrains.kotlin.fir.types.isAnyOrNullableAny
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.isNothingOrNullableNothing
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.type

object FirVarargWithNonTrivialUpperBoundInferredToNothingChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (expression.typeArguments.isEmpty()) return
        val resolvedSymbol = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        val typeParametersUsedAsVarargs = resolvedSymbol.valueParameterSymbols
            .filter { it.isVararg }
            .mapNotNull { valueParameter ->
                val varargType = valueParameter.resolvedReturnTypeRef.coneType.arrayElementTypeArgument()?.type ?: return@mapNotNull null
                varargType.toTypeParameterSymbol()
                    ?.takeUnless { it.isReified } // reified parameters are covered with FirReifiedChecker
            }
            .takeIf { it.isNotEmpty() }
            ?: return
        val substitutor = expression.createConeSubstitutorFromTypeArguments(context.session) ?: return
        for (typeParameter in typeParametersUsedAsVarargs) {
            val parameterType = typeParameter.toConeType()
            val actualVarargType = substitutor.substituteOrSelf(parameterType)
            val needReport = when {
                actualVarargType.isNothing -> true
                actualVarargType.isNullableNothing -> typeParameter.resolvedBounds.any { !it.coneType.isAnyOrNullableAny }
                else -> false
            }

            if (needReport) {
                val typeParameterIndex = resolvedSymbol.typeParameterSymbols.indexOf(typeParameter)
                val typeArgumentSource = typeParameterIndex.let { expression.typeArguments.getOrNull(it) }?.source
                val source = typeArgumentSource ?: expression.source
                reporter.reportOn(source, FirErrors.ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING, actualVarargType)
            }
        }
    }
}
