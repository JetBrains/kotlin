/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import kotlin.reflect.full.memberProperties

object FirInlineCallsInPlaceLambdaRestrictionsChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val report = IEReporter(expression.source, context, reporter, FirErrors.IE_DIAGNOSTIC)
        val variableSymbol = expression.calleeReference.toResolvedVariableSymbol() ?: return
        if (!variableSymbol.isVar) return

        val containingLambda = context.containingDeclarations.filterIsInstance<FirAnonymousFunctionSymbol>().lastOrNull() ?: return

        val lambdaSource = containingLambda.fir.source
        val variableSource = variableSymbol.fir.source
        if (lambdaSource != null && variableSource != null) {
            val vStart = variableSource.startOffset
            val lStart = lambdaSource.startOffset
            val lEnd = lambdaSource.endOffset
            if (vStart in lStart..<lEnd) return
        }

        if (!expression.partOfCall()) return

        val call = containingLambda.findContainingCall() ?: return
        val calledFunctionSymbol = call.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return
        if (calledFunctionSymbol.isInline && containingLambda.inlineStatus != InlineStatus.NoInline) return

        val contractDescription = calledFunctionSymbol.resolvedContractDescription
        val hasCallsInplace = contractDescription?.effects?.any { effectDeclaration ->
            val effect = effectDeclaration.effect
            effect is ConeCallsEffectDeclaration
        }
        if (hasCallsInplace != null && hasCallsInplace) return

        report(
            IEData(
                containingLambda = containingLambda.name.asString(),
                callName = calledFunctionSymbol.name.toString(),
                variableName = variableSymbol.name.toString(),
                varDeclaration = variableSymbol.fir.source?.lighterASTNode.toString()
            )
        )
    }

    context(context: CheckerContext)
    private fun FirExpression.partOfCall(): Boolean {
        val callsOrAssignments = context.callsOrAssignments
        val containingQualifiedAccess = callsOrAssignments.getOrNull(callsOrAssignments.size - 2) ?: return false
        if (this == (containingQualifiedAccess as? FirQualifiedAccessExpression)?.explicitReceiver?.unwrapErrorExpression()) return true
        val call = containingQualifiedAccess as? FirCall ?: return false
        val mapping = call.resolvedArgumentMapping ?: return false
        return mapping.keys.any { arg -> arg.unwrapErrorExpression().unwrapArgument() == this }
    }

    context(context: CheckerContext)
    private fun FirAnonymousFunctionSymbol.findContainingCall(): FirFunctionCall? {
        for (call in context.callsOrAssignments) {
            val functionCall = call as? FirFunctionCall ?: continue
            val mapping = functionCall.resolvedArgumentMapping ?: continue
            for ((argument, _) in mapping) {
                val anonymous = argument.unwrapErrorExpression()
                    .unwrapArgument() as? FirAnonymousFunctionExpression
                val af = anonymous?.anonymousFunction ?: continue
                if (af.symbol === this) return functionCall
            }
        }
        return null
    }
}

class IEReporter(
    private val source: KtSourceElement?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val error: KtDiagnosticFactory1<String>,
) {
    operator fun invoke(v: IEData) {
        val dataStr = buildList {
            addAll(serializeData(v))
        }.joinToString("; ")
        val str = "$borderTag $dataStr $borderTag"
        reporter.reportOn(source, error, str, context)
    }

    private val borderTag: String = "KLEKLE"

    private fun serializeData(v: IEData): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}

data class IEData(
    val containingLambda: String? = null,
    val callName: String? = null,
    val variableName: String? = null,
    val varDeclaration: String? = null,
)