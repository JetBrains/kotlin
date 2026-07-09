/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.evaluation

import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.shallowCopy
import org.jetbrains.kotlin.ir.visitors.IrVisitor

var IrConst.wasInlined: Boolean? by irAttribute(copyByDefault = true)

class IrConstFieldInliner(
    private val irFile: IrFile,
    private val inlineConstTracker: InlineConstTracker?,
) : IrVisitor<IrExpression?, Nothing?>() {
    override fun visitElement(element: IrElement, data: Nothing?): IrExpression? = null

    override fun visitConst(expression: IrConst, data: Nothing?): IrConst = expression

    override fun visitCall(expression: IrCall, data: Nothing?): IrExpression? {
        val field = expression.correspondingProperty?.backingField ?: return null
        if (!field.canBeInlined()) return null
        return expression.tryToInline(field)
    }

    override fun visitGetField(expression: IrGetField, data: Nothing?): IrExpression? {
        val field = expression.symbol.owner
        if (!field.canBeInlined()) return null
        return expression.tryToInline(field)
    }

    // Split the given expression into access to receiver (to keep semantic intact) and const value if applicable
    private fun IrExpression.tryToInline(field: IrField): IrExpression? {
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

    companion object {
        fun InlineConstTracker.reportOnIr(irFile: IrFile, field: IrField, value: IrConst) {
            if (field.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return

            val path = irFile.path
            val owner = field.parentAsClass.classId?.asString()?.replace(".", "$")?.replace("/", ".") ?: return
            val name = field.name.asString()
            val constType = value.kind.asString

            report(path, owner, name, constType)
        }
    }
}
