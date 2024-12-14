/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType

class JsSharedVariablesManager(
    private val builtIns: IrBuiltIns,
    private val dynamicType: IrDynamicType,
    intrinsics: JsIntrinsics,
) : SharedVariablesManager {

    private val createBox: IrSimpleFunctionSymbol = intrinsics.createSharedBox
    private val readBox: IrSimpleFunctionSymbol = intrinsics.readSharedBox
    private val writeBox: IrSimpleFunctionSymbol = intrinsics.writeSharedBox

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val valueType = originalDeclaration.type
        val initializer = originalDeclaration.initializer ?: IrConstImpl.constNull(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            valueType
        )

        val irCall =
            IrCallImpl(
                initializer.startOffset, initializer.endOffset,
                dynamicType, createBox,
                typeArgumentsCount = 1
            ).apply {
                typeArguments[0] = valueType
                putValueArgument(0, initializer)
            }

        return buildVariable(
            originalDeclaration.parent,
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            originalDeclaration.origin,
            originalDeclaration.name,
            dynamicType
        ).also {
            it.initializer = irCall
        }
    }

    override fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable) = sharedVariableDeclaration

    override fun getSharedValue(sharedVariableSymbol: IrValueSymbol, originalGet: IrGetValue): IrExpression {

        return IrCallImpl(
            originalGet.startOffset,
            originalGet.endOffset,
            originalGet.type,
            readBox,
            typeArgumentsCount = 1,
            originalGet.origin
        ).apply {
            typeArguments[0] = originalGet.type
            putValueArgument(
                0, IrGetValueImpl(
                    originalGet.startOffset,
                    originalGet.endOffset,
                    dynamicType,
                    sharedVariableSymbol,
                    originalGet.origin
                )
            )
        }
    }

    override fun setSharedValue(sharedVariableSymbol: IrValueSymbol, originalSet: IrSetValue): IrExpression {
        return IrCallImpl(
            originalSet.startOffset,
            originalSet.endOffset,
            builtIns.unitType,
            writeBox,
            typeArgumentsCount = 1,
            originalSet.origin
        ).apply {
            typeArguments[0] = originalSet.value.type
            putValueArgument(
                0, IrGetValueImpl(
                    originalSet.startOffset,
                    originalSet.endOffset,
                    dynamicType,
                    sharedVariableSymbol,
                    originalSet.origin
                )
            )
            putValueArgument(1, originalSet.value)
        }
    }
}
