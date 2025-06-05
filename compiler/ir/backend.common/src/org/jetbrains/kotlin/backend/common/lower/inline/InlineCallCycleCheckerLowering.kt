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
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@PhaseDescription("InlineCallCycleCheckerLowering")
class InlineCallCycleCheckerLowering<Context : PreSerializationLoweringContext>(val context: Context) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            context.diagnosticReporter.deduplicating(),
            context.configuration.languageVersionSettings
        )

        val callsInInlineCycle = mutableSetOf<IrCall>()
        irModule.acceptVoid(IrInlineCallCycleChecker(irDiagnosticReporter, callsInInlineCycle))
        irModule.accept(IrInlineCallCycleRemover(callsInInlineCycle), null)
    }
}

class IrInlineCallCycleChecker(
    private val diagnosticReporter: IrDiagnosticReporter,
    private val callsInInlineCycle: MutableSet<IrCall>,
) : IrVisitorVoid() {
    private val inlineCallsAndDeclarationsStack = mutableListOf<Any>()
    private val inlineDeclarationsBeingProcessed = mutableSetOf<IrFunction>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.body?.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        if (expression in callsInInlineCycle) return
        val callee = expression.symbol.owner
        if (!callee.isInline || !canEnterInlineDeclaration(expression, callee)) return

        enterInlineDeclaration(expression, callee)
        visitFunction(callee)
        exitInlineDeclaration()

        return super.visitCall(expression)
    }


    private fun canEnterInlineDeclaration(element: IrCall, callee: IrFunction): Boolean {
        if (callee in inlineDeclarationsBeingProcessed) {
            reportInlineCallCycle(element, callee)
            for ((call, callee) in inlineCallsAndDeclarationsStack.dropWhile { it != callee }.zipWithNext()) {
                if (call is IrCall && callee is IrFunction) {
                    reportInlineCallCycle(call, callee)
                }
            }
            return false
        }
        return true
    }

    private fun reportInlineCallCycle(element: IrCall, callee: IrFunction) {
        callsInInlineCycle.add(element)
        diagnosticReporter.at(element, callee.file).report(CommonBackendErrors.INLINE_CALL_CYCLE, callee.name)

    }

    private fun enterInlineDeclaration(call: IrCall, callee: IrFunction) {
        inlineCallsAndDeclarationsStack.add(call)
        inlineCallsAndDeclarationsStack.add(callee)
        inlineDeclarationsBeingProcessed.add(callee)
    }

    private fun exitInlineDeclaration() {
        inlineDeclarationsBeingProcessed.remove(inlineCallsAndDeclarationsStack.removeLast())
        inlineCallsAndDeclarationsStack.removeLast()
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
