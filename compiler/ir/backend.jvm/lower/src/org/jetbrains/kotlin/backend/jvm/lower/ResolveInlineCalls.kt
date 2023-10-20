/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.isMultifileBridge
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

internal val resolveInlineCallsPhase = makeIrModulePhase(
    ::ResolveInlineCalls,
    name = "ResolveInlineCalls",
    description = "Statically resolve calls to inline methods to particular implementations"
)

class ResolveInlineCalls(val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {
    override fun lower(irFile: IrFile) = irFile.acceptChildren(this, null)

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildren(this, null)

        if (!expression.symbol.owner.isInlineFunctionCall(context)) return
        val maybeFakeOverrideOfMultiFileBridge = expression.symbol.owner
        val resolved =
            maybeFakeOverrideOfMultiFileBridge.resolveMultiFileFacadeMember() ?: maybeFakeOverrideOfMultiFileBridge.resolveFakeOverride()
            ?: return

        expression.symbol = resolved.symbol
        expression.dispatchReceiver?.let { receiver ->
            val receiverType = resolved.parentAsClass.defaultType
            expression.dispatchReceiver = IrTypeOperatorCallImpl(
                receiver.startOffset,
                receiver.endOffset,
                receiverType,
                IrTypeOperator.IMPLICIT_CAST,
                receiverType,
                receiver
            )
        }
    }

    private fun IrFunction.resolveMultiFileFacadeMember(): IrSimpleFunction? =
        if (isMultifileBridge()) context.multifileFacadeMemberToPartMember[this] else null
}
