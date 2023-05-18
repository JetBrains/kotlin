/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class WasmArrayConstructorLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(ArrayConstructorTransformer(context, container as IrSymbolOwner))
    }
}

private class ArrayConstructorTransformer(
    val context: WasmBackendContext,
    val container: IrSymbolOwner
) : IrElementTransformerVoidWithContext() {
    // Array(size, init) -> create###Array(size, init)
    companion object {
        internal fun arrayInlineToSizeCreator(context: WasmBackendContext, irConstructor: IrConstructor): IrFunctionSymbol? =
            when (irConstructor.valueParameters.size) {
                2 -> context.wasmSymbols.primitiveTypeToCreateTypedArray[irConstructor.constructedClass.symbol]
                else -> null
            }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val creator = arrayInlineToSizeCreator(context, expression.symbol.owner)
            ?: return super.visitConstructorCall(expression)

        expression.transformChildrenVoid()

        val scope = (currentScope ?: createScope(container)).scope
        return context.createIrBuilder(scope.scopeOwnerSymbol).irBlock(expression.startOffset, expression.endOffset) {
            +irCall(creator, expression.type).also { call ->
                repeat(expression.typeArgumentsCount) { call.putTypeArgument(it, expression.getTypeArgument(it)) }
                repeat(expression.valueArgumentsCount) { call.putValueArgument(it, expression.getValueArgument(it)) }
            }
        }
    }
}

class WasmArrayConstructorReferenceLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(ArrayConstructorReferenceTransformer(context))
    }

    private class ArrayConstructorReferenceTransformer(val context: WasmBackendContext) : IrElementTransformerVoid() {
        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            expression.transformChildrenVoid()
            val target = expression.symbol.owner

            if (target !is IrConstructor) return expression

            val creator = ArrayConstructorTransformer.arrayInlineToSizeCreator(context, target)
                ?: return super.visitFunctionReference(expression)

            return IrFunctionReferenceImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                symbol = creator,
                typeArgumentsCount = expression.typeArgumentsCount,
                valueArgumentsCount = expression.valueArgumentsCount,
                reflectionTarget = creator,
                origin = expression.origin
            ).also { reference ->
                repeat(expression.typeArgumentsCount) { reference.putTypeArgument(it, expression.getTypeArgument(it)) }
                repeat(expression.valueArgumentsCount) { reference.putValueArgument(it, expression.getValueArgument(it)) }
            }
        }
    }
}