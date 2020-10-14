/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

@Suppress("RemoveExplicitTypeArguments")
internal val resolveInlineCallsPhase = makeIrModulePhase(
    ::ResolveInlineCalls,
    name = "ResolveInlineCalls",
    description = "Statically resolve calls to inline methods to particular implementations"
)

class ResolveInlineCalls(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitCall(expression: IrCall): IrExpression {
        if (!expression.symbol.owner.isInlineFunctionCall(context))
            return super.visitCall(expression)
        val maybeFakeOverrideOfMultiFileBridge = expression.symbol.owner as? IrSimpleFunction
            ?: return super.visitCall(expression)
        val resolved =
            maybeFakeOverrideOfMultiFileBridge.resolveMultiFileFacades() ?: maybeFakeOverrideOfMultiFileBridge.resolveFakeOverride()
            ?: return super.visitCall(expression)
        return super.visitCall(with(expression) {
            IrCallImpl(
                startOffset,
                endOffset,
                type,
                resolved.symbol,
                expression.typeArgumentsCount,
                expression.valueArgumentsCount,
                expression.origin,
                superQualifierSymbol
            ).apply {
                copyTypeAndValueArgumentsFrom(expression)
                dispatchReceiver?.let { receiver ->
                    val receiverType = resolved.parentAsClass.defaultType
                    dispatchReceiver = IrTypeOperatorCallImpl(
                        receiver.startOffset,
                        receiver.endOffset,
                        receiverType,
                        IrTypeOperator.IMPLICIT_CAST,
                        receiverType,
                        receiver
                    )
                }
            }
        })
    }

    private fun IrFunction.resolveMultiFileFacades(): IrSimpleFunction? =
        if (origin == JvmLoweredDeclarationOrigin.MULTIFILE_BRIDGE) {
            context.multifileFacadeMemberToPartMember[this]
        } else null
}
