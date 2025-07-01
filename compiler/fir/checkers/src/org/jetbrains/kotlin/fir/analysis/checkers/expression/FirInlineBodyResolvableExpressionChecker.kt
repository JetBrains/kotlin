/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isInlinable
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirInlineBodyResolvableExpressionChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        val inlinableParameterContext = context.inlinableParameterContext ?: return
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext
        if (expression !is FirQualifiedAccessExpression && expression !is FirDelegatedConstructorCall) return
        val targetSymbol = expression.toResolvedCallableSymbol() ?: return
        inlineFunctionBodyContext?.check(expression, targetSymbol)
        inlinableParameterContext.check(expression, targetSymbol)
    }

    class InlinableParameterContext(
        private val inlineFunction: FirFunction,
        private val inlinableParameters: List<FirValueParameterSymbol>,
        private val session: FirSession,
    ) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        fun check(qualifiedAccess: FirStatement, targetSymbol: FirCallableSymbol<*>) {
            val source = qualifiedAccess.source ?: return

            if (targetSymbol in inlinableParameters) {
                if (!qualifiedAccess.partOfCall()) {
                    reporter.reportOn(source, FirErrors.USAGE_IS_NOT_INLINABLE, targetSymbol)
                }
                if (context.containingDeclarations.any { it in inlinableParameters }) {
                    reporter.reportOn(
                        source,
                        FirErrors.NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE,
                        targetSymbol as FirValueParameterSymbol
                    )
                }
            }

            if (qualifiedAccess is FirQualifiedAccessExpression) {
                checkReceiver(qualifiedAccess, qualifiedAccess.dispatchReceiver, targetSymbol)
                checkReceiver(qualifiedAccess, qualifiedAccess.extensionReceiver, targetSymbol)
            }

            if (qualifiedAccess is FirFunctionCall) {
                checkArgumentsOfCall(qualifiedAccess, targetSymbol)
            }
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkReceiver(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            receiverExpression: FirExpression?,
            targetSymbol: FirBasedSymbol<*>,
        ) {
            if (receiverExpression == null) return
            val receiverSymbol =
                receiverExpression.unwrapErrorExpression().toResolvedCallableSymbol(session) as? FirValueParameterSymbol ?: return
            if (receiverSymbol in inlinableParameters) {
                if (!targetSymbol.isInvokeOfSomeFunctionType() || qualifiedAccessExpression is FirCallableReferenceAccess) {
                    reporter.reportOn(
                        receiverExpression.source ?: qualifiedAccessExpression.source,
                        FirErrors.USAGE_IS_NOT_INLINABLE,
                        receiverSymbol,
                    )
                } else if (!receiverSymbol.isCrossinline && !isNonLocalReturnAllowed()) {
                    reporter.reportOn(
                        receiverExpression.source ?: qualifiedAccessExpression.source,
                        FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED,
                        receiverSymbol,
                    )
                }
            }
        }

        context(context: CheckerContext)
        private fun isNonLocalReturnAllowed(): Boolean {
            val declarations = context.containingDeclarations
            val inlineFunctionIndex = declarations.indexOf(inlineFunction.symbol)
            if (inlineFunctionIndex == -1) return true

            for (i in (inlineFunctionIndex + 1) until declarations.size) {
                val declaration = declarations[i]

                // Only consider containers which can change locality.
                if (declaration !is FirFunctionSymbol && declaration !is FirClassSymbol) continue

                // Anonymous functions are allowed if they are an argument to an inline function call,
                // and the associated anonymous function parameter allows non-local returns. Everything
                // else changes locality, and must not be allowed.
                val anonymousFunction = declaration as? FirAnonymousFunctionSymbol ?: return false
                val (call, parameter) = extractCallAndParameter(anonymousFunction) ?: return false
                val callable = call.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return false
                if (!callable.isInline && !callable.isArrayLambdaConstructor()) return false
                if (parameter.isNoinline || parameter.isCrossinline) return false
            }

            return true
        }

        context(context: CheckerContext)
        private fun extractCallAndParameter(anonymousFunction: FirAnonymousFunctionSymbol): Pair<FirFunctionCall, FirValueParameter>? {
            for (call in context.callsOrAssignments) {
                if (call is FirFunctionCall) {
                    val mapping = call.resolvedArgumentMapping ?: continue
                    for ((argument, parameter) in mapping) {
                        if ((argument.unwrapArgument() as? FirAnonymousFunctionExpression)?.anonymousFunction?.symbol === anonymousFunction) {
                            return call to parameter
                        }
                    }
                }
            }
            return null
        }

        /**
         * @return true if the symbol is the constructor of one of 9 array classes (`Array<T>`,
         * `IntArray`, `FloatArray`, ...) which takes the size and an initializer lambda as parameters.
         * Such constructors are marked as `inline` but they are not loaded as such because the `inline`
         * flag is not stored for constructors in the binary metadata. Therefore, we pretend that they
         * are inline.
         */
        private fun FirFunctionSymbol<*>.isArrayLambdaConstructor(): Boolean {
            return this is FirConstructorSymbol &&
                    valueParameterSymbols.size == 2 &&
                    resolvedReturnType.isArrayOrPrimitiveArray
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkArgumentsOfCall(
            functionCall: FirFunctionCall,
            targetSymbol: FirBasedSymbol<*>,
        ) {
            if (context.isContractBody) return
            val calledFunctionSymbol = targetSymbol as? FirFunctionSymbol ?: return
            val argumentMapping = functionCall.resolvedArgumentMapping ?: return
            for ((wrappedArgument, valueParameter) in argumentMapping) {
                val argument = wrappedArgument.unwrapErrorExpression().unwrapArgument()
                val resolvedArgumentSymbol = argument.toResolvedCallableSymbol(session) as? FirVariableSymbol<*> ?: continue

                val valueParameterOfOriginalInlineFunction = inlinableParameters.firstOrNull { it == resolvedArgumentSymbol }
                if (valueParameterOfOriginalInlineFunction != null) {
                    val factory = when {
                        calledFunctionSymbol.isInline -> when {
                            !valueParameter.isInlinable(session) -> {
                                FirErrors.USAGE_IS_NOT_INLINABLE
                            }
                            !valueParameterOfOriginalInlineFunction.isCrossinline &&
                                    (valueParameter.isCrossinline || !isNonLocalReturnAllowed()) -> {
                                FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED
                            }
                            else -> continue
                        }
                        else -> FirErrors.USAGE_IS_NOT_INLINABLE
                    }
                    reporter.reportOn(argument.source, factory, valueParameterOfOriginalInlineFunction)
                }
            }
        }

        private fun FirBasedSymbol<*>.isInvokeOfSomeFunctionType(): Boolean {
            if (this !is FirNamedFunctionSymbol) return false
            return this.name == OperatorNameConventions.INVOKE &&
                    this.dispatchReceiverType?.isSomeFunctionType(session) == true
        }

        context(context: CheckerContext)
        private fun FirStatement.partOfCall(): Boolean {
            if (this !is FirExpression) return false
            val containingQualifiedAccess = context.callsOrAssignments.getOrNull(
                context.callsOrAssignments.size - 2
            ) ?: return false
            if (this == (containingQualifiedAccess as? FirQualifiedAccessExpression)?.explicitReceiver?.unwrapErrorExpression()) return true
            val call = containingQualifiedAccess as? FirCall ?: return false
            return call.arguments.any { it.unwrapErrorExpression().unwrapArgument() == this }
        }
    }
}

fun createInlinableParameterContext(function: FirFunction, session: FirSession): FirInlineBodyResolvableExpressionChecker.InlinableParameterContext {
    val inlinableParameters = function.valueParameters.mapNotNull { p -> p.takeIf { it.isInlinable(session) }?.symbol }
    return FirInlineBodyResolvableExpressionChecker.InlinableParameterContext(function, inlinableParameters, session)
}