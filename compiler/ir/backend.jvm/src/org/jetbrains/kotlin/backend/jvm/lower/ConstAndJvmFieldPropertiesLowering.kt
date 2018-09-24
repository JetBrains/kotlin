/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetterCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetterCallImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class ConstAndJvmFieldPropertiesLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {

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
        val irProperty = (expression.symbol.owner as? IrSimpleFunction)?.correspondingProperty ?: return super.visitCall(expression)

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

    private fun substituteSetter(irProperty: IrProperty, expression: IrCall): IrSetFieldImpl {
        return IrSetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            irProperty.backingField!!.symbol,
            expression.dispatchReceiver,
            expression.getValueArgument(expression.symbol.owner.valueParameters.lastIndex)!!,
            expression.type,
            expression.origin,
            expression.superQualifier?.let { context.ir.symbols.externalSymbolTable.referenceClass(it) }
        )
    }

    private fun substituteGetter(irProperty: IrProperty, expression: IrCall): IrExpression {
        return IrGetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            irProperty.backingField!!.symbol,
            expression.type,
            expression.dispatchReceiver,
            expression.origin,
            expression.superQualifier?.let { context.ir.symbols.externalSymbolTable.referenceClass(it) }
        )
    }
}
