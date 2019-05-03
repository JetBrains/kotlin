/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME

internal val propertiesToFieldsPhase = makeIrFilePhase(
    ::PropertiesToFieldsLowering,
    name = "PropertiesToFields",
    description = "Replace calls to default property accessors with field access and remove those accessors"
)

class PropertiesToFieldsLowering(val context: CommonBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (declaration.isConst || shouldSubstituteAccessorWithField(declaration, declaration.getter)) {
            declaration.getter = null
        }
        if (declaration.isConst || shouldSubstituteAccessorWithField(declaration, declaration.setter)) {
            declaration.setter = null
        }
        return super.visitProperty(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val simpleFunction = (expression.symbol.owner as? IrSimpleFunction) ?: return super.visitCall(expression)
        val property = simpleFunction.correspondingProperty ?: return super.visitCall(expression)

        if (shouldSubstituteAccessorWithField(property, simpleFunction)) {
            when (expression) {
                is IrGetterCallImpl -> return substituteGetter(property, expression)
                is IrSetterCallImpl -> return substituteSetter(property, expression)
            }
        }

        return super.visitCall(expression)
    }

    private fun shouldSubstituteAccessorWithField(property: IrProperty, accessor: IrSimpleFunction?): Boolean {
        if (accessor == null) return false

        // In contrast to the old backend, we do generate getters for lateinit properties, which fixes KT-28331
        if (property.isLateinit) return false

        if ((property.parent as? IrClass)?.kind == ClassKind.ANNOTATION_CLASS) return false

        if (property.backingField?.hasAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME) == true) return true

        return accessor.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR && Visibilities.isPrivate(accessor.visibility)
    }

    private fun substituteSetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        val backingField = irProperty.backingField!!
        val receiver = expression.dispatchReceiver?.transform(this, null)
        val setExpr = IrSetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            backingField.symbol,
            receiver,
            expression.getValueArgument(expression.valueArgumentsCount - 1)!!.transform(this, null),
            expression.type,
            expression.origin,
            expression.superQualifierSymbol
        )
        return buildSubstitution(backingField.isStatic, setExpr, receiver)
    }

    private fun substituteGetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        val backingField = irProperty.backingField!!
        val receiver = expression.dispatchReceiver?.transform(this, null)
        val getExpr = IrGetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            backingField.symbol,
            expression.type,
            receiver,
            expression.origin,
            expression.superQualifierSymbol
        )
        return buildSubstitution(backingField.isStatic, getExpr, receiver)
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
