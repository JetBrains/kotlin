/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.ir.util.defaultValueForType

class WasmSharedVariablesManager(val context: WasmBackendContext) : SharedVariablesManager {
    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val initializer = originalDeclaration.initializer ?: IrConstImpl.defaultValueForType(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            originalDeclaration.type
        )

        val boxClass = context.wasmSymbols.findClosureBoxClass(originalDeclaration.type)
        val constructorSymbol = boxClass.constructors.first()

        val irCall =
            IrConstructorCallImpl(
                startOffset = initializer.startOffset,
                endOffset = initializer.endOffset,
                type = boxClass.defaultType,
                symbol = constructorSymbol,
                typeArgumentsCount = boxClass.owner.typeParameters.size,
                constructorTypeArgumentsCount = constructorSymbol.owner.typeParameters.size,
                valueArgumentsCount = constructorSymbol.owner.valueParameters.size
            ).apply {
                putValueArgument(0, initializer)
            }

        return IrVariableImpl(
            startOffset = originalDeclaration.startOffset,
            endOffset = originalDeclaration.endOffset,
            origin = originalDeclaration.origin,
            symbol = IrVariableSymbolImpl(),
            name = originalDeclaration.name,
            type = irCall.type,
            isVar = false,
            isConst = false,
            isLateinit = false
        ).also {
            it.parent = originalDeclaration.parent
            it.initializer = irCall
        }
    }

    override fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable) = sharedVariableDeclaration

    override fun getSharedValue(sharedVariableSymbol: IrValueSymbol, originalGet: IrGetValue): IrExpression {
        val boxClass = sharedVariableSymbol.owner.type.classOrFail.owner
        val valueProperty = boxClass.declarations.firstIsInstance<IrProperty>()

        check(valueProperty.name.asString() == "value")
        val propertyGetter = valueProperty.getter!!

        val propertyGet = IrCallImpl(
            startOffset = originalGet.startOffset,
            endOffset = originalGet.endOffset,
            type = propertyGetter.returnType,
            symbol = propertyGetter.symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 0,
            origin = originalGet.origin
        ).also {
            it.dispatchReceiver = IrGetValueImpl(
                startOffset = originalGet.startOffset,
                endOffset = originalGet.endOffset,
                type = boxClass.defaultType,
                symbol = sharedVariableSymbol,
                origin = originalGet.origin
            )
        }

        return IrTypeOperatorCallImpl(
            startOffset = originalGet.startOffset,
            endOffset = originalGet.endOffset,
            type = originalGet.type,
            operator = IrTypeOperator.IMPLICIT_CAST,
            typeOperand = originalGet.type,
            argument = propertyGet
        )
    }

    override fun setSharedValue(sharedVariableSymbol: IrValueSymbol, originalSet: IrSetValue): IrExpression {
        val boxClass = sharedVariableSymbol.owner.type.classOrFail.owner
        val valueProperty = boxClass.declarations.firstIsInstance<IrProperty>()

        check(valueProperty.name.asString() == "value")
        val propertySetter = valueProperty.setter!!

        val propertySet = IrCallImpl(
            startOffset = originalSet.startOffset,
            endOffset = originalSet.endOffset,
            type = propertySetter.returnType,
            symbol = propertySetter.symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 1,
            origin = originalSet.origin
        ).also {
            it.dispatchReceiver = IrGetValueImpl(
                startOffset = originalSet.startOffset,
                endOffset = originalSet.endOffset,
                type = boxClass.defaultType,
                symbol = sharedVariableSymbol,
                origin = originalSet.origin
            )
            it.putValueArgument(0, originalSet.value)
        }

        return propertySet
    }
}
