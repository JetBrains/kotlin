/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrVisitor

internal data class CallNode(val function: IrFunction, val callLocation: IrBody)

internal data class CallEdge(val call: IrCall?, val callNode: CallNode)

class InlineCallCycleCheckerLowering<Context : LoweringContext>(val context: Context) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val callGraph = mutableMapOf<CallNode, MutableSet<CallEdge>>()

        irModule.accept(IrInlineCallGraphBuilder(callGraph), null)
        val diagnosticReporter = (context as? PreSerializationLoweringContext)?.diagnosticReporter
        traverseCallGraph(callGraph, diagnosticReporter)
    }

    private fun traverseCallGraph(
        callGraph: MutableMap<CallNode, MutableSet<CallEdge>>,
        diagnosticReporter: IrDiagnosticReporter?,
    ) {
        val visited = mutableSetOf<CallNode>()
        val completed = mutableSetOf<CallNode>()
        val inlineCallsStack = mutableListOf<CallEdge>()

        fun reportInlineCallCycle(edgesInCycle: List<CallEdge>) {
            if (diagnosticReporter != null) {
                (edgesInCycle + edgesInCycle.first()).zipWithNext().forEach { (callerEdge, calleeEdge) ->
                    calleeEdge.call?.let { call ->
                        diagnosticReporter
                            .at(call, callerEdge.callNode.function.file)
                            .report(CommonBackendErrors.INLINE_CALL_CYCLE, calleeEdge.callNode.function)
                    }
                }
            } else {
                val errorMessage = buildString {
                    appendLine("Inline cycle detected:")
                    for (edge in edgesInCycle) {
                        appendLine("The '${edge.callNode.function.fqNameWhenAvailable}' invocation is a part of inline cycle",)
                    }
                }
                context.messageCollector.report(CompilerMessageSeverity.ERROR, errorMessage)
                throw CompilationErrorException()
            }
        }

        fun CallNode.dfs(call: IrCall?) {
            if (visited.contains(this)) {
                if (!completed.contains(this)) {
                    val edgesInCycle = inlineCallsStack.takeLastWhile { (_, callNode) -> callNode != this } + CallEdge(call, this)
                    reportInlineCallCycle(edgesInCycle)
                }
                return
            }

            inlineCallsStack += CallEdge(call, this)
            visited += this

            callGraph[this]?.forEach { (call, node) -> node.dfs(call) }

            inlineCallsStack.removeLast()
            completed += this

        }

        callGraph.keys.forEach { it.dfs(null) }
    }
}


internal class IrInlineCallGraphBuilder(
    private val callGraph: MutableMap<CallNode, MutableSet<CallEdge>>,
) : IrVisitor<Unit, CallNode?>() {
    override fun visitElement(element: IrElement, data: CallNode?) {
        element.acceptChildren(this, data)
    }

    override fun visitFunction(declaration: IrFunction, data: CallNode?) {
        if (declaration.isInline) {
            if (data != null) {
                addEdges(data, null, declaration)
            }

            declaration.parameters.mapNotNull { it.defaultValue }.forEach { it.accept(this, CallNode(declaration, it)) }
            declaration.body?.let { it.accept(this, CallNode(declaration, it)) }
        } else {
            visitElement(declaration, data)
        }
    }

    override fun visitCall(expression: IrCall, data: CallNode?) {
        val callee = expression.symbol.owner
        if (!callee.isInline) return visitElement(expression, data)

        data?.let { addEdges(it, expression, callee) }

        visitElement(expression, data)
    }

    private fun addEdges(callerNode: CallNode, call: IrCall?, callee: IrFunction) {
        val usedDefaultValues =
            callee.parameters.filter { it.defaultValue != null && call?.arguments[it] == null }.mapNotNull { it.defaultValue }

        val callNodes =
            (usedDefaultValues + callee.body).filterNotNull().map { CallEdge(call, CallNode(callee, it)) }

        callGraph.getOrPut(callerNode) { mutableSetOf() }.addAll(callNodes)
    }
}
