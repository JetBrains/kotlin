/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrVisitor

internal data class CallNode(val function: IrFunction, val callLocation: IrBody)

internal data class CallEdge(val call: IrCall?, val callNode: CallNode)

class InlineCallCycleCheckerLowering<Context : LoweringContext>(val context: Context) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) { TODO() }

    private fun traverseCallGraph(
        callGraph: MutableMap<CallNode, MutableSet<CallEdge>>,
    ) { TODO() }
}


internal class IrInlineCallGraphBuilder(
    private val callGraph: MutableMap<CallNode, MutableSet<CallEdge>>,
) : IrVisitor<Unit, CallNode?>() {
    override fun visitElement(element: IrElement, data: CallNode?) { TODO() }

    override fun visitFunction(declaration: IrFunction, data: CallNode?) { TODO() }

    override fun visitCall(expression: IrCall, data: CallNode?) { TODO() }

    private fun addEdges(callerNode: CallNode, call: IrCall?, callee: IrFunction) { TODO() }
}
