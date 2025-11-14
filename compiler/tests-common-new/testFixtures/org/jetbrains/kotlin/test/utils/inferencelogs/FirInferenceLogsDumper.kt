/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils.inferencelogs

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger.BlockOwner
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger.Companion.sanitizeFqNames
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger.BlockElement
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger.FixationLogVariableInfo
import org.jetbrains.kotlin.resolve.calls.inference.components.LegacyVariableReadinessCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableReadinessCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableReadinessCalculator.TypeVariableFixationReadinessQuality
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.types.model.TypeVariableMarker

abstract class FirInferenceLogsDumper {
    abstract fun renderDump(sessionsToLoggers: Map<FirSession, FirInferenceLogger>): String

    protected abstract fun monospace(text: String): String

    protected abstract fun formatCode(code: Any): String

    protected fun ConeTypeParameterBasedTypeVariable.renderReferenceToPrototype(): String {
        @OptIn(SymbolInternals::class)
        val fir = typeParameterSymbol.containingDeclarationSymbol.fir as? FirTypeParameterRefsOwner
        val index = fir?.typeParameters?.indexOfFirst { it.symbol == typeParameterSymbol }

        return when {
            index == null || index == -1 -> monospace(typeParameterSymbol.containingDeclarationSymbol.toString())
            else -> monospace(typeParameterSymbol.containingDeclarationSymbol.toString()) + "s parameter $index"
        }
    }

    protected fun renderErrorTitle(error: ConstraintSystemError): String {
        val naiveString = sanitizeFqNames(error.toString())
        val customRepresentation = when {
            naiveString == error::class.simpleName -> ""
            else -> ": " + formatCode(naiveString)
        }
        return error::class.simpleName + customRepresentation
    }

    protected fun renderVariableTitle(variable: TypeVariableMarker): String {
        val variableInfo = when (variable) {
            is ConeTypeParameterBasedTypeVariable -> " for " + variable.renderReferenceToPrototype()
            is ConeTypeVariableForLambdaParameterType -> " for lambda parameter"
            is ConeTypeVariableForLambdaReturnType -> " for lambda return type"
            is ConeTypeVariableForPostponedAtom -> " for postponed argument"
            else -> ""
        }
        return "New " + monospace(variable.toString()) + variableInfo
    }

    protected inline fun <T, R> List<T>.renderNotNullWithIndex(render: T.(Int) -> R?): List<R>? {
        var index = 0

        return mapNotNull {
            it.render(index)?.also { index++ }
        }.takeIf { it.isNotEmpty() }
    }

    sealed class PrintingNode

    class CallNode(
        val call: FirInferenceLogger.Call,
        val index: Int,
        val candidates: MutableList<CandidateNode> = mutableListOf(),
    ) : PrintingNode()

    class CandidateNode(
        val owner: BlockOwner.Candidate,
        val index: Int,
        val blocks: MutableList<BlockElement> = mutableListOf(),
    ) : PrintingNode()

    class PassthroughNode(val element: FirInferenceLogger.LoggingElement) : PrintingNode()

    /**
     * Combines some consecutive blocks into a tree to simplify printing.
     * This is handy for assigning proper indices and avoiding printing the
     * same header multiple times.
     */
    protected fun buildAdditionalPrintingStructure(topLevelElements: List<BlockElement>): MutableList<PrintingNode> {
        val callIndices = mutableMapOf<FirElement, Int>()
        val candidateIndices = mutableMapOf<FirElement?, MutableMap<BlockOwner, Int>>()
        val topLevelNodes = mutableListOf<PrintingNode>()

        for (element in topLevelElements) {
            val owner = element.owner

            if (owner !is BlockOwner.Candidate) {
                topLevelNodes.add(PassthroughNode(element))
                continue
            }

            val owningCall = owner.owningCall
            val previousCall = topLevelNodes.lastOrNull() as? CallNode
            val previousCandidate = previousCall?.candidates?.lastOrNull() ?: topLevelNodes.lastOrNull() as? CandidateNode

            if (owner == previousCandidate?.owner) {
                previousCandidate.blocks.add(element)
                continue
            }

            val candidateIndex = candidateIndices
                .getOrPut(owningCall.fir) { mutableMapOf() }
                .let { it.getOrPut(owner) { it.size } }

            val candidate = CandidateNode(owner, candidateIndex, mutableListOf(element))

            if (owningCall.fir == previousCall?.call?.fir) {
                previousCall.candidates.add(candidate)
                continue
            }

            val callIndex = callIndices.getOrPut(owningCall.fir) { callIndices.size }
            val call = CallNode(owningCall, callIndex, mutableListOf(candidate))
            topLevelNodes += call
        }

        return topLevelNodes
    }

    /**
     * This function accepts a [FixationLogVariableInfo] instead of the `readiness` itself
     * because the `readiness` property is not type-safe.
     */
    protected fun FixationLogVariableInfo<*>.renderReadiness(paddingSize: Int = 0): String {
        val linePadding = " ".repeat(paddingSize)

        return when (val readiness = readiness) {
            is VariableReadinessCalculator.TypeVariableFixationReadiness -> {
                val qualities = TypeVariableFixationReadinessQuality.entries.joinToString("") {
                    "\n$linePadding\t" + (if (readiness[it]) " true " else "false ") + it.name
                }
                "Readiness($qualities\n$linePadding)"
            }
            is LegacyVariableReadinessCalculator.TypeVariableFixationReadiness -> readiness.toString()
            else -> error("Unexpected readiness type: ${readiness::class}")
        }
    }
}
