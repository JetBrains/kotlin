/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.preprocessor

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.types.classOrFail

class IrInterpreterConstGetterPreprocessor : IrInterpreterPreprocessor {
    override fun visitFunction(declaration: IrFunction, data: IrInterpreterPreprocessorData): IrStatement {
        // It is useless to visit default accessor, we probably want to leave code there as it is
        if (declaration.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) return declaration
        return super.visitFunction(declaration, data)
    }

    override fun visitCall(expression: IrCall, data: IrInterpreterPreprocessorData): IrElement {
        val function = (expression.symbol.owner as? IrSimpleFunction) ?: return super.visitCall(expression, data)
        val field = function.correspondingPropertySymbol?.owner?.backingField ?: return super.visitCall(expression, data)
        return expression.lowerConstRead(field, data) ?: super.visitCall(expression, data)
    }

    override fun visitGetField(expression: IrGetField, data: IrInterpreterPreprocessorData): IrExpression {
        return expression.lowerConstRead(expression.symbol.owner, data) ?: super.visitGetField(expression, data)
    }

    private fun IrExpression.lowerConstRead(field: IrField, data: IrInterpreterPreprocessorData): IrExpression? {
        val receiver = when (this) {
            is IrCall -> dispatchReceiver
            is IrGetField -> receiver
            else -> return null
        }

        if (receiver == null || !field.hasConstantValue()) return null

        transformChildren(this@IrInterpreterConstGetterPreprocessor, data)

        val getObject = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, receiver.type, receiver.type.classOrFail)
        when (this) {
            is IrCall -> this.dispatchReceiver = getObject
            is IrGetField -> this.receiver = getObject
        }

        return if (receiver.shouldDropConstReceiver()) {
            this
        } else {
            IrCompositeImpl(startOffset, endOffset, this.type, null, listOf(receiver, this))
        }
    }

    private fun IrExpression.shouldDropConstReceiver(): Boolean {
        return this is IrGetValue || this is IrGetObjectValue
    }

    fun IrField.hasConstantValue(): Boolean {
        val implicitConst = isFinal && isStatic && origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && initializer != null
        return implicitConst || correspondingPropertySymbol?.owner?.isConst == true
    }
}