/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorCallExpressionImpl
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitor

// `callLocation` is either `IrBody` or `IrValueArgument`
internal data class CallNode(val function: IrFunction, val callLocation: IrElement)

internal data class CallEdge(val call: IrCall?, val callNode: CallNode)

@PhaseDescription("InlineCallCycleCheckerLowering")
class InlineCallCycleCheckerLowering<Context : PreSerializationLoweringContext>(val context: Context) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            context.diagnosticReporter.deduplicating(),
            context.configuration.languageVersionSettings
        )

        val callsInInlineCycle = mutableSetOf<IrCall>()
        val callGraph = mutableMapOf<CallNode, MutableSet<CallEdge>>()

        irModule.accept(IrInlineCallGraphBuilder(callGraph), listOf())
        traverseCallGraph(callGraph, irDiagnosticReporter, callsInInlineCycle)
        irModule.accept(IrInlineCallCycleRemover(callsInInlineCycle), null)
    }

    private fun traverseCallGraph(
        callGraph: MutableMap<CallNode, MutableSet<CallEdge>>,
        diagnosticReporter: IrDiagnosticReporter,
        callsInInlineCycle: MutableSet<IrCall>,
    ) {
        val visited = mutableSetOf<CallNode>()
        val completed = mutableSetOf<CallNode>()
        val inlineCallsStack = mutableListOf<CallEdge>()

        fun reportInlineCallCycle(element: IrCall?, callee: IrFunction) {
            if (element == null) return
            callsInInlineCycle.add(element)
            diagnosticReporter.at(element, callee.file).report(CommonBackendErrors.INLINE_CALL_CYCLE, callee.name)
        }

        fun CallNode.dfs(call: IrCall?) {
            if (visited.contains(this)) {
                if (!completed.contains(this)) {
                    reportInlineCallCycle(call, this.function)
                    inlineCallsStack.takeLastWhile { (_, callNode) -> callNode != this }.forEach { (call, callNode) ->
                        reportInlineCallCycle(call, callNode.function)
                    }
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
) : IrVisitor<Unit, List<CallNode>>() {
    override fun visitElement(element: IrElement, data: List<CallNode>) {
        element.acceptChildren(this, data)
    }

    override fun visitFunction(declaration: IrFunction, data: List<CallNode>) {
        if (declaration.isInline) {
            declaration.parameters.forEach { it.accept(this, data + CallNode(declaration, it)) }
            declaration.body?.let { it.accept(this, data + CallNode(declaration, it)) }
        } else {
            super.visitFunction(declaration, data)
        }
    }

    override fun visitCall(expression: IrCall, data: List<CallNode>) {
        val callee = expression.symbol.owner
        if (!callee.isInline) return super.visitCall(expression, data)

        data.forEach { addEdges(it, expression, callee) }

        return super.visitCall(expression, data)
    }

    private fun addEdges(callerNode: CallNode, call: IrCall, callee: IrFunction) {
        val parametersUsingDefaultValues =
            callee.parameters.filter { it.defaultValue != null && call.arguments[it] == null }

        val callNodes =
            (parametersUsingDefaultValues + callee.body).filterNotNull().map { CallEdge(call, CallNode(callee, it)) }

        callGraph.getOrPut(callerNode) { mutableSetOf() }.addAll(callNodes)
    }
}

class IrInlineCallCycleRemover(private val callsInInlineCycle: MutableSet<IrCall>) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression in callsInInlineCycle) return IrErrorCallExpressionImpl(
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = expression.type,
            description = "'${expression.render()}' is a part of an inline call cycle"
        )
        return super.visitCall(expression)
    }
}
