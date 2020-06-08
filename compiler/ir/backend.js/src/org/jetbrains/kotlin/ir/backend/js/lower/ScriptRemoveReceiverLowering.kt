/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import java.lang.IllegalArgumentException

class ScriptRemoveReceiverLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.scriptMode) {
            irFile.declarations.transformFlat {
                if (it is IrScript) {
                    lower(it)
                } else null
            }
        }
    }

    private fun IrExpression.nullConst() = IrConstImpl.constNull(startOffset, endOffset, type.makeNullable())

    @OptIn(DescriptorBasedIr::class)
    fun lower(script: IrScript): List<IrScript> {
        val transformer: IrElementTransformerVoid = object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.parent is IrScript) {
                    expression.dispatchReceiver = null
                }
                return super.visitCall(expression)
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return if (expression.symbol === script.thisReceiver.symbol) expression.nullConst()
                else expression
            }

            override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
                if (expression.symbol.owner.parent is IrScript) {
                    expression.receiver = null
                }
                return super.visitFieldAccess(expression)
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.symbol.owner.parent is IrScript) {
                    expression.dispatchReceiver = null

                    val result = with(super.visitFunctionReference(expression) as IrFunctionReference) {
                        val arguments = (type as IrSimpleType).arguments.filter {
                            !(it is IrTypeProjection && it.type is IrSimpleType && (it.type as IrSimpleType).classifier.descriptor is ScriptDescriptor)
                        }
                        IrFunctionReferenceImpl(
                            startOffset,
                            endOffset,
                            IrSimpleTypeImpl(
                                context.ir.symbols.functionN(arguments.size),
                                (type as IrSimpleType).hasQuestionMark,
                                arguments,
                                type.annotations
                            ),
                            symbol,
                            typeArgumentsCount,
                            valueArgumentsCount,
                            reflectionTarget,
                            origin
                        ).also {
                            it.dispatchReceiver = dispatchReceiver
                            it.extensionReceiver = extensionReceiver
                        }
                    }

                    return result
                }
                return expression
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)


                if (expression.symbol.owner.parent is IrScript) {
                    expression.dispatchReceiver = null

                    val result = with(super.visitPropertyReference(expression) as IrPropertyReference) {
                        val arguments = (type as IrSimpleType).arguments.filter {
                            !(it is IrTypeProjection && it.type is IrSimpleType && (it.type as IrSimpleType).classifier.descriptor is ScriptDescriptor)
                        }
                        IrPropertyReferenceImpl(
                            startOffset,
                            endOffset,
                            IrSimpleTypeImpl(
                                (if (setter == null) getPropertyN(arguments.size) else getMutablePropertyN(arguments.size)),
                                (type as IrSimpleType).hasQuestionMark,
                                arguments,
                                type.annotations
                            ),
                            symbol,
                            typeArgumentsCount,
                            field,
                            getter,
                            setter,
                            origin
                        ).also {
                            it.dispatchReceiver = dispatchReceiver
                            it.extensionReceiver = extensionReceiver
                        }
                    }

                    return result
                }
                return expression
            }

            private fun getPropertyN(n: Int): IrClassSymbol {
                return when (n) {
                    2 -> context.ir.symbols.kproperty2()
                    1 -> context.ir.symbols.kproperty1()
                    0 -> context.ir.symbols.kproperty0()
                    else -> throw IllegalArgumentException()
                }
            }

            private fun getMutablePropertyN(n: Int): IrClassSymbol {
                return when (n) {
                    2 -> context.ir.symbols.kmutableproperty2()
                    1 -> context.ir.symbols.kmutableproperty1()
                    0 -> context.ir.symbols.kmutableproperty0()
                    else -> throw IllegalArgumentException()
                }
            }
        }

        script.transformChildrenVoid(transformer)

        script.declarations.forEach {
            when (it) {
                is IrSimpleFunction -> it.dispatchReceiverParameter = null
                is IrProperty -> {
                    it.getter?.dispatchReceiverParameter = null
                    it.setter?.dispatchReceiverParameter = null
                }
            }
        }

        return listOf(script)
    }
}
