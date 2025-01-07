/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.asInlinable
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Transforms `Array(size) { index -> value }` into a loop.
 */
@PhaseDescription(name = "ArrayConstructor")
class ArrayConstructorLowering(private val context: LoweringContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(ArrayConstructorTransformer(context, container as IrSymbolOwner))
    }
}

private class ArrayConstructorTransformer(
    val context: LoweringContext,
    val container: IrSymbolOwner
) : IrElementTransformerVoidWithContext() {

    // Array(size, init) -> Array(size)
    companion object {
        internal fun arrayInlineToSizeConstructor(context: LoweringContext, irConstructor: IrConstructor): IrFunctionSymbol? {
            val clazz = irConstructor.constructedClass.symbol
            return when {
                irConstructor.parameters.size != 2 -> null
                clazz == context.irBuiltIns.arrayClass -> context.ir.symbols.arrayOfNulls // Array<T> has no unary constructor: it can only exist for Array<T?>
                context.irBuiltIns.primitiveArraysToPrimitiveTypes.contains(clazz) -> clazz.constructors.single {
                    it.owner.hasShape(regularParameters = 1, parameterTypes = listOf(context.irBuiltIns.intType))
                }
                else -> null
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val sizeConstructor = arrayInlineToSizeConstructor(context, expression.symbol.owner)
            ?: return super.visitConstructorCall(expression)
        // inline fun <reified T> Array(size: Int, invokable: (Int) -> T): Array<T> {
        //     val result = arrayOfNulls<T>(size)
        //     for (i in 0 until size) {
        //         result[i] = invokable(i)
        //     }
        //     return result as Array<T>
        // }
        // (and similar for primitive arrays)
        val size = expression.arguments[0]!!.transform(this, null)
        val invokable = expression.arguments[1]!!.transform(this, null)
        if (invokable.type.isNothing()) {
            // Expressions of type 'Nothing' don't terminate.
            return invokable
        }
        val scope = (currentScope ?: createScope(container)).scope
        return context.createIrBuilder(scope.scopeOwnerSymbol).irBlock(expression.startOffset, expression.endOffset) {
            val index = createTmpVariable(irInt(0), isMutable = true)
            val sizeVar = createTmpVariable(size)
            val result = createTmpVariable(irCall(sizeConstructor, expression.type).apply {
                copyTypeArgumentsFrom(expression)
                arguments[0] = irGet(sizeVar)
            })

            val generator = invokable.asInlinable(this)
            +irWhile().apply {
                condition = irCall(context.irBuiltIns.lessFunByOperandType[index.type.classifierOrFail]!!).apply {
                    arguments[0] = irGet(index)
                    arguments[1] = irGet(sizeVar)
                }
                body = irBlock {
                    val tempIndex = createTmpVariable(irGet(index))
                    +irCall(result.type.getClass()!!.functions.single { it.name == OperatorNameConventions.SET }).apply {
                        arguments[0] = irGet(result)
                        arguments[1] = irGet(tempIndex)
                        arguments[2] = generator.inline(parent, listOf(tempIndex))
                    }
                    val inc = index.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INC }
                    +irSet(
                        index.symbol,
                        irCallOp(inc.symbol, index.type, irGet(index)),
                        origin = IrStatementOrigin.PREFIX_INCR
                    )
                }
            }
            +irGet(result)
        }
    }
}
