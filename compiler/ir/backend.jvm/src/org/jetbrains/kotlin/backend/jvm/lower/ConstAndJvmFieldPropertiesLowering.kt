/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class ConstAndJvmFieldPropertiesLowering(val context: CommonBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (JvmCodegenUtil.isConstOrHasJvmFieldAnnotation(declaration.descriptor)) {
            /*Safe or need copy?*/
            declaration.getter = null
            declaration.setter = null
        }
        return super.visitProperty(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val irSimpleFunction = (expression.symbol.owner as? IrSimpleFunction) ?: return super.visitCall(expression)
        val irProperty = irSimpleFunction.correspondingProperty ?: return super.visitCall(expression)

        if (irProperty.isConst) {
            (irProperty.backingField?.initializer?.expression as? IrConst<*>)?.let { return it }
        }

        if (irProperty.backingField?.hasAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME) == true) {
            return if (expression is IrGetterCallImpl) {
                substituteGetter(irProperty, expression)
            } else {
                assert(expression is IrSetterCallImpl)
                substituteSetter(irProperty, expression)
            }
        } else if (irProperty.descriptor is SyntheticJavaPropertyDescriptor) {
            expression.dispatchReceiver = expression.extensionReceiver
            expression.extensionReceiver = null
        }
        return super.visitCall(expression)
    }

    private fun substituteSetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        val backingField = irProperty.backingField!!
        val receiver = expression.dispatchReceiver?.let { super.visitExpression(it) }
        val setExpr = IrSetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            backingField.symbol,
            receiver,
            super.visitExpression(expression.getValueArgument(expression.valueArgumentsCount - 1)!!),
            expression.type,
            expression.origin,
            expression.superQualifierSymbol
        )
        return buildSubstitution(backingField.isStaticField(), setExpr, receiver)
    }

    private fun substituteGetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        val backingField = irProperty.backingField!!
        val receiver = expression.dispatchReceiver?.let { super.visitExpression(it) }
        val getExpr = IrGetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            backingField.symbol,
            expression.type,
            receiver,
            expression.origin,
            expression.superQualifierSymbol
        )
        return buildSubstitution(backingField.isStaticField(), getExpr, receiver)
    }

    private fun buildSubstitution(needBlock: Boolean, setOrGetExpr: IrFieldAccessExpression, receiver: IrExpression?): IrExpression {
        if (receiver != null && needBlock) {
            // Evaluate `dispatchReceiver` for the sake of its side effects, then return `setOrGetExpr`.
            return context.createIrBuilder(setOrGetExpr.symbol, setOrGetExpr.startOffset, setOrGetExpr.endOffset).irBlock(setOrGetExpr) {
                // `coerceToUnit()` is private in InsertImplicitCasts, have to reproduce it here
                val receiverVoid = IrTypeOperatorCallImpl(
                    receiver.startOffset, receiver.endOffset,
                    context.irBuiltIns.unitType,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                    context.irBuiltIns.unitType, context.irBuiltIns.unitType.classifierOrFail,
                    receiver
                )

                +receiverVoid
                setOrGetExpr.receiver = null
                +setOrGetExpr
            }
        } else {
            // Just `setOrGetExpr` (`dispatchReceiver` is evaluated as a subexpression thereof)
            return setOrGetExpr
        }
    }
}

// Should probably be an utility function with a more direct implementation.
private fun IrField.isStaticField() = (descriptor.dispatchReceiverParameter == null)
