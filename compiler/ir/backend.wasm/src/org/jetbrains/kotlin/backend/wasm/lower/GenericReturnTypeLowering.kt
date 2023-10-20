/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.erasedUpperBound
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This lowering adds implicit casts in places where erased generic function return type
 * differs from expected type on the call site.
 */
class GenericReturnTypeLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression =
                transformGenericCall(
                    super.visitCall(expression) as IrCall,
                    currentScope!!.scope.scopeOwnerSymbol
                )
        })
    }

    private fun IrType.eraseUpperBoundType(): IrType {
        val type = erasedUpperBound?.defaultType ?: return context.irBuiltIns.anyNType
        return if (this.isNullable())
            type.makeNullable()
        else
            type
    }

    private fun transformGenericCall(call: IrCall, scopeOwnerSymbol: IrSymbol): IrExpression {
        val function = call.symbol.owner

        val erasedReturnType: IrType =
            function.realOverrideTarget.returnType.eraseUpperBoundType()

        val callType = call.type

        if (erasedReturnType != call.type) {
            if (callType.isNothing()) return call
            if (erasedReturnType.isSubtypeOf(callType, context.typeSystem)) return call

            // Erase type parameter from call return type
            val newCall = irCall(
                call,
                function.symbol,
                newReturnType = erasedReturnType,
                newSuperQualifierSymbol = call.superQualifierSymbol
            )

            context.createIrBuilder(scopeOwnerSymbol).apply {
                return irImplicitCast(newCall, call.type)
            }
        }
        return call
    }
}
