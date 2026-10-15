/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.isTrivialGetter
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

open class PropertyAccessorInlineLowering(
    private val context: CommonBackendContext,
) : BodyLoweringPass {

    fun IrProperty.isSafeToInlineInClosedWorld() =
        isTopLevel || (modality === Modality.FINAL || visibility == DescriptorVisibilities.PRIVATE) || (parent as IrClass).modality === Modality.FINAL

    open fun IrProperty.isSafeToInline(accessContainer: IrDeclaration): Boolean =
        isSafeToInlineInClosedWorld()

    // TODO: implement general function inlining optimization and replace it with
    private inner class AccessorInliner(val container: IrDeclaration) : IrElementTransformerVoid() {

        private val unitType = context.irBuiltIns.unitType

        private fun canBeInlined(callee: IrSimpleFunction): Boolean {
            val property = callee.correspondingPropertySymbol?.owner ?: return false

            // Some de-virtualization required here
            if (!property.isSafeToInline(container)) return false

            val parent = property.parent
            if (parent is IrClass) {
                // TODO: temporary workarounds
                if (parent.isExpect || property.isExpect) return false
                if (parent.parent is IrExternalPackageFragment) return false
                if (context.inlineClassesUtils.isClassInlineLike(parent)) return false
            }
            if (property.isEffectivelyExternal()) return false
            return true
        }

        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)

            val callee = expression.symbol.owner

            if (!canBeInlined(callee)) return expression

            var analyzedCallee = callee
            while (analyzedCallee.isFakeOverride) {
                analyzedCallee = analyzedCallee.resolveFakeOverride() ?: return expression
            }

            if (!canBeInlined(analyzedCallee)) return expression

            val property = analyzedCallee.correspondingPropertySymbol?.owner ?: return expression

            val backingField = property.backingField ?: return expression

            if (property.isConst) {
                val initializer =
                    (backingField.initializer ?: error("Constant property has to have a backing field with initializer"))
                val constExpression = initializer.expression.deepCopyWithSymbols()
                val receiver = expression.dispatchReceiver
                if (receiver != null && !receiver.isPure(true)) {
                    val builder = context.createIrBuilder(
                        expression.symbol,
                        expression.startOffset, expression.endOffset
                    )
                    return builder.irBlock(expression) {
                        +receiver
                        +constExpression
                    }
                }
                return constExpression
            }



            if (property.getter === analyzedCallee) {
                return tryInlineSimpleGetter(expression, analyzedCallee, backingField) ?: expression
            }

            if (property.setter === analyzedCallee) {
                return tryInlineSimpleSetter(expression, analyzedCallee, backingField) ?: expression
            }

            return expression
        }

        private fun tryInlineSimpleGetter(call: IrCall, callee: IrSimpleFunction, backingField: IrField): IrExpression? {
            if (!callee.isTrivialGetter) return null

            val builder = context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)

            val getField = call.run {
                IrGetFieldImpl(startOffset, endOffset, backingField.symbol, backingField.type, call.dispatchReceiver, origin)
            }

            // Preserve call types when backingField have different type. This usually happens with generic field types.
            return if (backingField.type != call.type)
                builder.irImplicitCast(getField, call.type)
            else
                getField
        }

        private fun tryInlineSimpleSetter(call: IrCall, callee: IrSimpleFunction, backingField: IrField): IrExpression? {
            if (!isSimpleSetter(callee, backingField)) return null

            return call.run {
                val value = arguments.last() ?: error("Setter should have a value argument")
                IrSetFieldImpl(startOffset, endOffset, backingField.symbol, call.dispatchReceiver, value, unitType, origin)
            }
        }

        private fun isSimpleSetter(callee: IrSimpleFunction, backingField: IrField): Boolean {
            val body = callee.body?.let { it as IrBlockBody } ?: return false
            val statementsSizeCheck = when (body.statements.size) {
                1 -> true
                // In K/N backend this lowering should be called after devirtualization. At this point IrReturns are already added.
                2 -> (body.statements[1] as? IrReturn)?.value?.type?.isUnit() == true
                else -> false
            }
            if (!statementsSizeCheck) return false
            val stmt = body.statements[0]
            val setFieldStmt = stmt as? IrSetField ?: return false
            if (setFieldStmt.symbol !== backingField.symbol) return false

            // TODO: support constant setters
            val setValue = setFieldStmt.value as? IrGetValue ?: return false
            val valueSymbol = callee.parameters.single { it.kind == IrParameterKind.Regular }.symbol
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
        irBody.transformChildrenVoid(AccessorInliner(container))
    }
}
