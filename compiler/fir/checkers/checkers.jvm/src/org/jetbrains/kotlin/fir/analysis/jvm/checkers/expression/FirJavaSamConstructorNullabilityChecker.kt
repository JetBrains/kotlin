/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

object FirJavaSamConstructorNullabilityChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.JavaTypeParameterDefaultRepresentationWithDNN)) return

        val calleeReference = expression.calleeReference
        if (calleeReference.isError()) return
        val symbol = calleeReference.toResolvedFunctionSymbol() ?: return
        if (symbol.origin != FirDeclarationOrigin.SamConstructor) return
        if (symbol.resolvedReturnType.toRegularClassSymbol(context.session)?.isJavaOrEnhancement != true) return

        val (lambda, parameter) = expression.resolvedArgumentMapping?.entries?.singleOrNull() ?: return
        if (lambda !is FirAnonymousFunctionExpression) return

        val parameterFunctionType = parameter.returnTypeRef.coneType
        val substitutor = expression.createConeSubstitutorFromTypeArguments(symbol, context.session)
        val expectedReturnType = parameterFunctionType.typeArguments.lastOrNull()?.type?.let(substitutor::substituteOrSelf) ?: return

        for (returnedExpression in lambda.getReturnedExpressions()) {
            val returnedExpressionType = returnedExpression.resolvedType
            if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, returnedExpressionType, expectedReturnType)) {
                reporter.reportOn(
                    returnedExpression.source,
                    FirJvmErrors.TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES,
                    expectedReturnType,
                    returnedExpressionType,
                    context,
                )
            }
        }
    }

    private fun FirAnonymousFunctionExpression.getReturnedExpressions(): List<FirExpression> {
        val exitNode = anonymousFunction.controlFlowGraphReference?.controlFlowGraph?.exitNode ?: return emptyList()

        fun extractReturnedExpression(it: CFGNode<*>): FirExpression? {
            return when (it) {
                is JumpNode -> (it.fir as? FirReturnExpression)?.result
                is BlockExitNode -> (it.fir.statements.lastOrNull() as? FirReturnExpression)?.result
                is FinallyBlockExitNode -> {
                    val finallyBlockEnterNode =
                        generateSequence(it, CFGNode<*>::lastPreviousNode).firstIsInstanceOrNull<FinallyBlockEnterNode>() ?: return null
                    finallyBlockEnterNode.previousNodes.firstOrNull { x -> finallyBlockEnterNode.edgeFrom(x) == exitNode.edgeFrom(it) }
                        ?.let(::extractReturnedExpression)
                }
                else -> null
            }
        }

        return exitNode.previousNodes.mapNotNull(::extractReturnedExpression)
    }
}