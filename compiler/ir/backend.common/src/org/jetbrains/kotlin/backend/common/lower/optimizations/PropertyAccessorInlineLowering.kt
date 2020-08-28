/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class PropertyAccessorInlineLowering(private val context: CommonBackendContext) : BodyLoweringPass {

    private val IrProperty.isSafeToInline: Boolean get() = isTopLevel || (modality === Modality.FINAL || visibility == Visibilities.PRIVATE) || (parent as IrClass).modality === Modality.FINAL

    // TODO: implement general function inlining optimization and replace it with
    private inner class AccessorInliner : IrElementTransformerVoid() {

        private val unitType = context.irBuiltIns.unitType

        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)

            val callee = expression.symbol.owner
            val property = callee.correspondingPropertySymbol?.owner ?: return expression

            // Some devirtualization required here
            if (!property.isSafeToInline) return expression

            val parent = property.parent
            if (parent is IrClass) {
                // TODO: temporary workarounds
                if (parent.isExpect || property.isExpect) return expression
                if (parent.parent is IrExternalPackageFragment) return expression
                if (parent.isInline) return expression
            }
            if (property.isEffectivelyExternal()) return expression

            if (property.isConst) {
                val initializer =
                    (property.backingField?.initializer ?: error("Constant property has to have a backing field with initializer"))
                return initializer.expression.deepCopyWithSymbols()
            }

            val backingField = property.backingField ?: return expression

            if (property.getter === callee) {
                return tryInlineSimpleGetter(expression, callee, backingField) ?: expression
            }

            if (property.setter === callee) {
                return tryInlineSimpleSetter(expression, callee, backingField) ?: expression
            }

            return expression
        }

        private fun tryInlineSimpleGetter(call: IrCall, callee: IrSimpleFunction, backingField: IrField): IrExpression? {
            if (!isSimpleGetter(callee, backingField)) return null

            return call.run {
                IrGetFieldImpl(startOffset, endOffset, backingField.symbol, backingField.type, call.dispatchReceiver, origin)
            }
        }

        private fun isSimpleGetter(callee: IrSimpleFunction, backingField: IrField): Boolean {
            val body = callee.body?.let { it as IrBlockBody } ?: return false

            val stmt = body.statements.singleOrNull() ?: return false
            val returnStmt = stmt as? IrReturn ?: return false
            val getFieldStmt = returnStmt.value as? IrGetField ?: return false
            if (getFieldStmt.symbol !== backingField.symbol) return false
            val receiver = getFieldStmt.receiver

            if (receiver == null) {
                assert(callee.dispatchReceiverParameter == null)
                return true
            }

            if (receiver is IrGetValue) return receiver.symbol.owner === callee.dispatchReceiverParameter

            return false
        }

        private fun tryInlineSimpleSetter(call: IrCall, callee: IrSimpleFunction, backingField: IrField): IrExpression? {
            if (!isSimpleSetter(callee, backingField)) return null

            return call.run {
                val value = getValueArgument(0) ?: error("Setter should have a value argument")
                IrSetFieldImpl(startOffset, endOffset, backingField.symbol, call.dispatchReceiver, value, unitType, origin)
            }
        }

        private fun isSimpleSetter(callee: IrSimpleFunction, backingField: IrField): Boolean {
            val body = callee.body?.let { it as IrBlockBody } ?: return false

            val stmt = body.statements.singleOrNull() ?: return false
            val setFieldStmt = stmt as? IrSetField ?: return false
            if (setFieldStmt.symbol !== backingField.symbol) return false

            // TODO: support constant setters
            val setValue = setFieldStmt.value as? IrGetValue ?: return false
            val valueSymbol = callee.valueParameters.single().symbol
            if (setValue.symbol !== valueSymbol) return false

            val receiver = setFieldStmt.receiver

            if (receiver == null) {
                assert(callee.dispatchReceiverParameter == null)
                return true
            }

            if (receiver is IrGetValue) return receiver.symbol.owner === callee.dispatchReceiverParameter

            return false
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(AccessorInliner())
    }
}