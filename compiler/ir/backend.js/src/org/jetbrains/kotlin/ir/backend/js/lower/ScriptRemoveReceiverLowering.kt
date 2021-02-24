/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
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

            private fun isScript(it: IrTypeArgument) = it.typeOrNull?.classifierOrNull is IrScriptSymbol

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.symbol.owner.parent is IrScript) {
                    expression.dispatchReceiver = null

                    val result = with(super.visitFunctionReference(expression) as IrFunctionReference) {
                        // TODO do we really need to fix type or removing dispatchReceiver is enough?
                        val arguments = (type as IrSimpleType).arguments.filterNot(::isScript)
                        val newN = arguments.size - 1

                        IrFunctionReferenceImpl(
                            startOffset,
                            endOffset,
                            IrSimpleTypeImpl(
                                context.ir.symbols.functionN(newN),
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
                        // TODO do we really need to fix type or removing dispatchReceiver is enough?
                        val arguments = (type as IrSimpleType).arguments.filterNot(::isScript)
                        val newN = arguments.size - 1

                        IrPropertyReferenceImpl(
                            startOffset,
                            endOffset,
                            IrSimpleTypeImpl(
                                (if (setter == null) getPropertyN(newN) else getMutablePropertyN(newN)),
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

        script.statements.forEach {
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
