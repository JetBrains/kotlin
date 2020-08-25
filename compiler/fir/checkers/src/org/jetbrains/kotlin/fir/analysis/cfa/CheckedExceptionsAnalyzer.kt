package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.cfa.coeffect.CoeffectActionsCollector
import org.jetbrains.kotlin.fir.analysis.cfa.coeffect.CoeffectActionsOnNodes
import org.jetbrains.kotlin.fir.analysis.cfa.coeffect.CoeffectAnalyzer
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectContextActions
import org.jetbrains.kotlin.fir.contract.contextual.CoeffectFamily
import org.jetbrains.kotlin.fir.contract.contextual.diagnostics.CoeffectContextVerificationError
import org.jetbrains.kotlin.fir.contract.contextual.family.checkedexception.CatchesExceptionCoeffectContextProvider
import org.jetbrains.kotlin.fir.contract.contextual.family.checkedexception.CheckedExceptionCoeffectContextCleaner
import org.jetbrains.kotlin.fir.contract.contextual.family.checkedexception.CheckedExceptionCoeffectFamily
import org.jetbrains.kotlin.fir.contract.contextual.family.checkedexception.CheckedExceptionContextError
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryMainBlockEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryMainBlockExitNode
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object CheckedExceptionsAnalyzer : CoeffectAnalyzer() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val function = graph.declaration as? FirFunction<*> ?: return

        val lambdaToOwnerFunction = function.collectLambdaOwnerFunctions()
        val actionsOnNodes = graph.collectActionsOnNodes(CheckedExceptionsCollector(lambdaToOwnerFunction))
        if (!actionsOnNodes.hasVerifiers()) return
        val contextOnNodes = graph.collectContextOnNodes(actionsOnNodes)

        verifyCoeffectContext(actionsOnNodes, contextOnNodes, function.session) { node, error ->
            node.fir.source?.let {
                val firError = error.toFirError(it)
                reporter.report(firError)
            }
        }
    }

    class CheckedExceptionsCollector(
        lambdaToOwnerFunction: Map<FirAnonymousFunction, Pair<FirFunction<*>, AbstractFirBasedSymbol<*>>>
    ) : CoeffectActionsCollector(lambdaToOwnerFunction) {

        override fun collectFamilyActions(family: CoeffectFamily): Boolean = family === CheckedExceptionCoeffectFamily

        override fun visitTryMainBlockEnterNode(node: TryMainBlockEnterNode, data: CoeffectActionsOnNodes) {
            for (catch in node.fir.catches) {
                val exceptionType = catch.parameter.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: continue
                data[node] = CoeffectContextActions(provider = CatchesExceptionCoeffectContextProvider(exceptionType, catch))
            }
        }

        override fun visitTryMainBlockExitNode(node: TryMainBlockExitNode, data: CoeffectActionsOnNodes) {
            for (catch in node.fir.catches) {
                val exceptionType = catch.parameter.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: continue
                data[node] = CoeffectContextActions(cleaner = CheckedExceptionCoeffectContextCleaner(exceptionType, catch))
            }
        }
    }

    private fun CoeffectContextVerificationError.toFirError(source: FirSourceElement): FirDiagnostic<*>? = when (this) {
        is CheckedExceptionContextError -> FirErrors.UNCHECKED_EXCEPTION.on(source, exceptionType)
        else -> null
    }
}