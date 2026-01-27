/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationExitNode
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import kotlin.reflect.full.memberProperties


object FirCapturedMutableVariablesAnalyzer : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(data: VariableInitializationInfoData) {
        val capturedAliases = mutableSetOf<FirVariableSymbol<*>>()
        val containingLambda = data.graph.declaration as? FirAnonymousFunction ?: return
        val lambdaSymbol = containingLambda.symbol
        if (lambdaSymbol.inlineStatus == InlineStatus.Inline) return
        val invocationKind = containingLambda.invocationKind
        if (invocationKind.isInPlace) return
        for (node in data.graph.nodes) {
            updateAliases(node, data.properties, capturedAliases)
            checkUsage(node, containingLambda, data.properties, capturedAliases)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkUsage(
        node: CFGNode<*>,
        containingLambda: FirAnonymousFunction,
        trackedProperties: Set<FirVariableSymbol<*>>,
        capturedAliases: Set<FirVariableSymbol<*>>,
    ) {
        val (expression, variableSymbol) = when (node) {
            is QualifiedAccessNode -> node.fir to (node.fir.calleeReference.toResolvedVariableSymbol() ?: return)
            is VariableAssignmentNode -> node.fir to (node.fir.calleeReference?.toResolvedVariableSymbol() ?: return)
            else -> return
        }
        val source = expression.source ?: return
        if (variableSymbol.isVal) return

        if (variableSymbol.resolvedReturnType is ConeDynamicType) return
        val accessExpression = when (expression) {
            is FirQualifiedAccessExpression -> expression
            is FirVariableAssignment -> expression.lValue as? FirQualifiedAccessExpression
            else -> {
                return
            }
        }

        val leftmostReceiverSymbol = leftmostReceiverVariableSymbol(accessExpression)
        if (leftmostReceiverSymbol != null) {
            if (!leftmostReceiverSymbol.isLocal) return
            val isSymbolCaptured = isCaptured(variableSymbol, leftmostReceiverSymbol, trackedProperties, capturedAliases)
            if (!isSymbolCaptured) return
        } else {
            if (!variableSymbol.isLocal) return
            val isSymbolCaptured = isCaptured(variableSymbol, null, trackedProperties, capturedAliases)
            if (!isSymbolCaptured) return
        }

        val report = IEReporter(source, context, reporter, FirErrors.CV_DIAGNOSTIC)
        report(
            IEData(
                info = "Variable is captured from outer scope",
                containingLambda = containingLambda.symbol.name.toString(),
                variableName = variableSymbol.name.toString(),
                leftmostReceiverName = leftmostReceiverSymbol?.name.toString(),
            )
        )
    }

    private fun updateAliases(
        node: CFGNode<*>,
        trackedProperties: Set<FirVariableSymbol<*>>,
        capturedAliases: MutableSet<FirVariableSymbol<*>>,
    ) {
        val (lhsSymbol, rValue) = when (node) {
            is VariableDeclarationExitNode -> node.fir.symbol to node.fir.initializer
            is VariableAssignmentNode -> {
                val lhs = (node.fir.lValue as? FirQualifiedAccessExpression)
                    ?.calleeReference?.toResolvedVariableSymbol()
                lhs to node.fir.rValue
            }
            else -> return
        }
        if (lhsSymbol == null) return

        val isRhsCaptured = isOrContainsCapturedRef(rValue, trackedProperties, capturedAliases)
        if (isRhsCaptured) {
            capturedAliases.add(lhsSymbol)
        } else {
            capturedAliases.remove(lhsSymbol)
        }
    }

    private fun isOrContainsCapturedRef(
        expression: FirExpression?,
        trackedProperties: Set<FirVariableSymbol<*>>,
        capturedAliases: Set<FirVariableSymbol<*>>,
    ): Boolean {
        if (expression == null) {
            return false
        }
        val type = expression.resolvedType
        if (type.isPrimitiveOrNullablePrimitive || type.isString || type.isNullableString) return false

        return when (expression) {
            is FirFunctionCall -> false
            is FirQualifiedAccessExpression -> {
                val symbol = expression.calleeReference.toResolvedVariableSymbol() ?: return false

                val leftmostReceiver = leftmostReceiverVariableSymbol(expression)
                val isReceiverCaptured =
                    leftmostReceiver != null && isCaptured(leftmostReceiver, leftmostReceiver, trackedProperties, capturedAliases)
                val isSymbolCaptured = isCaptured(symbol, leftmostReceiver, trackedProperties, capturedAliases)

                if (isSymbolCaptured && symbol.isLocal) return true
                if (isReceiverCaptured && leftmostReceiver.isLocal) return true
                false
            }
            is FirWhenExpression -> {
                expression.branches.any { branch ->
                    isOrContainsCapturedRef(branch.result, trackedProperties, capturedAliases)
                }
            }
            is FirTryExpression -> {
                val tryBlockResult = expression.tryBlock.statements.lastOrNull() as? FirExpression
                val catchBlockResults = expression.catches.mapNotNull { it.block.statements.lastOrNull() as? FirExpression }

                (tryBlockResult != null && isOrContainsCapturedRef(tryBlockResult, trackedProperties, capturedAliases)) ||
                        catchBlockResults.any { isOrContainsCapturedRef(it, trackedProperties, capturedAliases) }
            }
            is FirCheckNotNullCall -> isOrContainsCapturedRef(expression.argument, trackedProperties, capturedAliases)
            is FirElvisExpression -> isOrContainsCapturedRef(expression.lhs, trackedProperties, capturedAliases) ||
                    isOrContainsCapturedRef(expression.rhs, trackedProperties, capturedAliases)
            is FirTypeOperatorCall -> isOrContainsCapturedRef(expression.argument, trackedProperties, capturedAliases)
            is FirSmartCastExpression -> isOrContainsCapturedRef(expression.originalExpression, trackedProperties, capturedAliases)
            is FirWrappedExpression -> isOrContainsCapturedRef(expression.expression, trackedProperties, capturedAliases)
            is FirBlock -> {
                val lastStatement = expression.statements.lastOrNull() as? FirExpression
                lastStatement != null && isOrContainsCapturedRef(lastStatement, trackedProperties, capturedAliases)
            }
            else -> false
        }
    }

    private fun leftmostReceiverVariableSymbol(expression: FirQualifiedAccessExpression?): FirVariableSymbol<*>? {
        if (expression == null) {
            return null
        }
        var current: FirExpression =
            expression.explicitReceiver?.unwrapErrorExpression()?.unwrapArgument() ?: return null
        while (true) {
            when (val e = current) {
                is FirQualifiedAccessExpression -> {
                    val next = e.explicitReceiver?.unwrapErrorExpression()?.unwrapArgument()
                    if (next != null) {
                        current = next
                        continue
                    }
                    return e.calleeReference.toResolvedVariableSymbol()
                }
                is FirSafeCallExpression -> {
                    current = e.receiver.unwrapErrorExpression().unwrapArgument()
                }
                is FirCheckNotNullCall -> {
                    current = e.argument.unwrapErrorExpression().unwrapArgument()
                }
                is FirSmartCastExpression -> {
                    current = e.originalExpression
                }
                else -> return null
            }
        }
    }


    private fun isCaptured(
        variableSymbol: FirVariableSymbol<*>,
        leftmostReceiverSymbol: FirVariableSymbol<*>?,
        trackedProperties: Set<FirVariableSymbol<*>>,
        capturedAliases: Set<FirVariableSymbol<*>>,
    ): Boolean {
        // Logic:
        // If a symbol is in [trackedProperties], it is declared inside the current lambda.
        // If a symbol is in [capturedAliases], it is an alias to a captured variable.
        if (leftmostReceiverSymbol != null && leftmostReceiverSymbol !in trackedProperties) return true
        if (leftmostReceiverSymbol == null && variableSymbol !in trackedProperties) return true
        if (leftmostReceiverSymbol in capturedAliases) return true
        if (variableSymbol in capturedAliases) return false
        return false
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
    val info: String? = null,
    val containingLambda: String? = null,
    val variableName: String? = null,
    val leftmostReceiverName: String? = null,
)