/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.ResultStatement
import org.jetbrains.kotlin.fir.resolve.dfa.VariableStorage
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.isReal
import org.jetbrains.kotlin.name.StandardClassIds

object FirResultAnalyzer : FirControlFlowChecker(MppCheckerKind.Common) {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        val variableStorage = VariableStorage(context.session)
        for (node in graph.nodes) {
            variableStorage.checkUnsafe(node, reporter, context)
        }
    }

    private fun VariableStorage.checkUnsafe(node: CFGNode<*>, reporter: DiagnosticReporter, context: CheckerContext) {
        val fir = (node as? QualifiedAccessNode)?.fir ?: return

        val callableId = fir.calleeReference.toResolvedVariableSymbol()?.callableId ?: return
        val required = when (callableId) {
            StandardClassIds.Callables.result -> ResultStatement.SUCCESS
            StandardClassIds.Callables.exception -> ResultStatement.FAILURE
            else -> return
        }

        val dispatchReceiver = fir.dispatchReceiver ?: return
        val realVariable = get(dispatchReceiver, createReal = true, node.flow::unwrapVariable)
        if (realVariable == null || !realVariable.isReal()) return
        val actual = node.flow.getTypeStatement(realVariable)?.resultStatement ?: ResultStatement.UNKNOWN

        if (required != actual) {
            reporter.reportOn(fir.source, FirErrors.UNSAFE_RESULT, context)
        }
    }
}