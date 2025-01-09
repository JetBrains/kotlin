/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createDelegatingCallWithPlaceholderTypeArguments
import org.jetbrains.kotlin.backend.jvm.ir.isDefinitelyNotDefaultImplsMethod
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isSuperToAny
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Redirects super interface calls to DefaultImpls.
 */
@PhaseDescription(name = "InterfaceSuperCalls")
internal class InterfaceSuperCallsLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val superQualifierClass = expression.superQualifierSymbol?.owner
        if (superQualifierClass == null || !superQualifierClass.isInterface || expression.isSuperToAny()) {
            return super.visitCall(expression)
        }

        val superCallee = expression.symbol.owner
        if (superCallee.isDefinitelyNotDefaultImplsMethod(context.config.jvmDefaultMode, superCallee.resolveFakeOverride()))
            return super.visitCall(expression)

        val redirectTarget = context.cachedDeclarations.getDefaultImplsFunction(superCallee)
        val newCall = createDelegatingCallWithPlaceholderTypeArguments(expression, redirectTarget, context.irBuiltIns)
        postprocessMovedThis(newCall)
        return super.visitCall(newCall)
    }

    private fun postprocessMovedThis(irCall: IrCall) {
        val movedThisParameter = irCall.symbol.owner.valueParameters
            .find { it.origin == IrDeclarationOrigin.Companion.MOVED_DISPATCH_RECEIVER }
            ?: return
        val movedThisParameterIndex = movedThisParameter.indexInOldValueParameters
        irCall.putValueArgument(
            movedThisParameterIndex,
            irCall.getValueArgument(movedThisParameterIndex)?.reinterpretAsDispatchReceiverOfType(movedThisParameter.type)
        )
    }
}

// Given a dispatch receiver expression, wrap it in REINTERPRET_CAST to the given type,
// unless it's a value of inline class (which could be boxed at this point).
// Avoids a CHECKCAST on a moved dispatch receiver argument.
internal fun IrExpression.reinterpretAsDispatchReceiverOfType(irType: IrType): IrExpression =
    if (this.type.isInlineClassType())
        this
    else
        IrTypeOperatorCallImpl(
            this.startOffset, this.endOffset,
            irType, IrTypeOperator.REINTERPRET_CAST, irType,
            this
        )
