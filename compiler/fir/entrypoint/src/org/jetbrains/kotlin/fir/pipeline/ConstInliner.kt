/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.interpreter.transformer.reportOnIr
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.shallowCopy
import org.jetbrains.kotlin.ir.visitors.IrTransformer

var IrConst.wasInlined: Boolean? by irAttribute(copyByDefault = true)

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ConstInliner(
    private val irFile: IrFile,
    private val inlineConstTracker: InlineConstTracker?,
) : IrTransformer<Nothing?>() {
    override fun visitFunction(declaration: IrFunction, data: Nothing?): IrStatement {
        // It is useless to visit default accessor, we probably want to leave code there as it is
        if (declaration.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) return declaration
        return visitDeclaration(declaration, data)
    }

    override fun visitCall(expression: IrCall, data: Nothing?): IrElement {
        return expression.correspondingProperty?.backingField?.let {
            expression.tryToInline(it)
        } ?: visitElement(expression, data)
    }

    override fun visitGetField(expression: IrGetField, data: Nothing?): IrExpression {
        val field = expression.symbol.owner
        return expression.tryToInline(field) ?: visitExpression(expression, data)
    }

    // Split the given expression into access to receiver (to keep semantic intact) and const value if applicable
    private fun IrExpression.tryToInline(field: IrField): IrExpression? {
        if (!field.canBeInlined()) return null

        transformChildren(this@ConstInliner, null)

        val receiver = when (this) {
            is IrCall -> dispatchReceiver
            is IrGetField -> receiver
            else -> return null
        }

        val const = field.getInitializerAndReportInlining(this)
        if (receiver == null || receiver.shouldDropConstReceiver()) return const

        val fieldParent = field.parentAsClass
        val getObject = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, fieldParent.defaultType, fieldParent.symbol)
        when (this) {
            is IrCall -> this.dispatchReceiver = getObject
            is IrGetField -> this.receiver = getObject
        }

        return IrCompositeImpl(startOffset, endOffset, this.type, null, listOf(receiver, const))
    }

    private fun IrExpression.shouldDropConstReceiver(): Boolean {
        return this is IrGetValue || this is IrGetObjectValue
    }

    fun IrField.isMarkedAsConst(): Boolean {
        val implicitConst = isFinal && isStatic && origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && initializer != null
        return implicitConst || this.property.isConst
    }

    private fun IrField.canBeInlined(): Boolean {
        val property = this.property ?: return false
        val initializer = property.backingField?.initializer?.expression
        return this.isMarkedAsConst() && initializer is IrConst
    }

    private fun IrField.getInitializerAndReportInlining(original: IrExpression): IrConst {
        val const = this.initializer?.expression as IrConst
        inlineConstTracker?.reportOnIr(irFile, this, const)
        return (const.shallowCopy() as IrConst).apply {
            startOffset = original.startOffset
            endOffset = original.endOffset
            wasInlined = true
        }
    }

    private val IrField.property: IrProperty?
        get() = this.correspondingPropertySymbol?.owner

    private val IrCall.correspondingProperty: IrProperty?
        get() = this.symbol.owner.correspondingPropertySymbol?.owner

    private val IrProperty?.isConst: Boolean
        get() = this?.isConst == true
}