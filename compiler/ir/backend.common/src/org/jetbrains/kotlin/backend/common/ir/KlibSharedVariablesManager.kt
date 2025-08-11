/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultValueForType

class KlibSharedVariablesManager(private val symbols: FrontendKlibSymbols): SharedVariablesManager() {
    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        return with(originalDeclaration) {
            val valueType = type
            val initializer = originalDeclaration.initializer
                ?: IrConstImpl.defaultValueForType(startOffset, endOffset, valueType)

            val boxClass = symbols.genericSharedVariableBox
            val boxVariableType = boxClass.klass.typeWith(valueType)
            val irCall = IrConstructorCallImpl(
                initializer.startOffset,
                initializer.endOffset,
                boxVariableType,
                boxClass.constructor,
                typeArgumentsCount = 1,
                constructorTypeArgumentsCount = 0,
            ).apply {
                typeArguments[0] = valueType
                arguments[0] = initializer
            }
            IrVariableImpl(
                startOffset,
                endOffset,
                origin,
                IrVariableSymbolImpl(),
                name,
                boxVariableType,
                isVar = false,
                isConst = false,
                isLateinit = false,
            ).also {
                it.initializer = irCall
            }
        }
    }


    override fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable): IrStatement =
        sharedVariableDeclaration

    override fun getSharedValue(sharedVariableSymbol: IrValueSymbol, originalGet: IrGetValue): IrExpression =
        with(originalGet) {
            IrCallImpl(
                startOffset,
                endOffset,
                type,
                symbols.genericSharedVariableBox.load,
                typeArgumentsCount = 0,
                origin,
            ).also {
                it.arguments[0] = IrGetValueImpl(startOffset, endOffset, sharedVariableSymbol.owner.type, sharedVariableSymbol)
            }
        }

    override fun setSharedValue(sharedVariableSymbol: IrValueSymbol, originalSet: IrSetValue): IrExpression =
        with(originalSet) {
            IrCallImpl(
                startOffset,
                endOffset,
                originalSet.type,
                symbols.genericSharedVariableBox.store,
                typeArgumentsCount = 0,
                origin,
            ).also {
                it.arguments[0] = IrGetValueImpl(startOffset, endOffset, sharedVariableSymbol.owner.type, sharedVariableSymbol)
                it.arguments[1] = value
            }
        }
}
