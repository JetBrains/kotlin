/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.resolveFakeOverride
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isReal
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class LateinitLowering(
        val context: Context,
        private val generateParameterNameInAssertion: Boolean = false
) : FileLoweringPass {

    private val isInitializedGetter = context.ir.symbols.isInitializedGetter

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrBuildingTransformer(context) {

            override fun visitVariable(declaration: IrVariable): IrStatement {
                declaration.transformChildrenVoid(this)

                if (!declaration.isLateinit) return declaration

                assert(declaration.initializer == null) {
                    "'lateinit' modifier is not allowed for variables with initializer"
                }
                builder.at(declaration).run {
                    declaration.initializer = irNull()
                }
                return declaration
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val symbol = expression.symbol
                if (symbol !is IrVariableSymbol || !symbol.owner.isLateinit) return expression

                builder.at(expression).run {
                    return irBlock(expression) {
                        // TODO: do data flow analysis to check if value is proved to be not-null.
                        +irIfThen(
                                irEqualsNull(irGet(expression.type, symbol)),
                                throwUninitializedPropertyAccessException(symbol.owner.name)
                        )
                        +irGet(expression.type, symbol)
                    }
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.symbol != isInitializedGetter) return expression

                val propertyReference = expression.extensionReceiver!! as IrPropertyReference
                assert(propertyReference.extensionReceiver == null) {
                    "'lateinit' modifier is not allowed on extension properties"
                }
                val getter = propertyReference.getter?.owner ?: TODO(propertyReference.dump())
                val property = getter.resolveFakeOverride().correspondingProperty!!

                builder.at(expression).run {
                    val field = property.backingField!!
                    val fieldValue = irGetField(propertyReference.dispatchReceiver, field)
                    return irNotEquals(fieldValue, irNull())
                }
            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                declaration.transformChildrenVoid(this)

                if (!declaration.isLateinit || !declaration.isReal)
                    return declaration

                val backingField = declaration.backingField!!
                transformGetter(backingField, declaration.getter!!)

                assert(backingField.initializer == null) {
                    "'lateinit' modifier is not allowed for properties with initializer"
                }
                val irBuilder = context.createIrBuilder(backingField.symbol, declaration.startOffset, declaration.endOffset)
                irBuilder.run {
                    backingField.initializer = irExprBody(irNull())
                }

                return declaration
            }

            private fun transformGetter(backingField: IrField, getter: IrFunction) {
                val irBuilder = context.createIrBuilder(getter.symbol, getter.startOffset, getter.endOffset)
                irBuilder.run {
                    getter.body = irBlockBody {
                        val resultVar = irTemporary(
                                irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField)
                        )
                        +irIfThenElse(
                                context.irBuiltIns.nothingType,
                                irNotEquals(irGet(resultVar), irNull()),
                                irReturn(irGet(resultVar)),
                                throwUninitializedPropertyAccessException(backingField.name)
                        )
                    }
                }
            }
        })
    }

    private fun IrBuilderWithScope.throwUninitializedPropertyAccessException(name: Name) =
            irCall(throwErrorFunction, context.irBuiltIns.nothingType).apply {
                if (generateParameterNameInAssertion) {
                    putValueArgument(
                            0,
                            IrConstImpl.string(
                                    startOffset,
                                    endOffset,
                                    context.irBuiltIns.stringType,
                                    name.asString()
                            )
                    )
                }
            }

    private val throwErrorFunction = context.ir.symbols.ThrowUninitializedPropertyAccessException

}