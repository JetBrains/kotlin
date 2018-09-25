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
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
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
        val descriptor = expression.descriptor as? PropertyAccessorDescriptor ?: return super.visitCall(expression)

        val property = descriptor.correspondingProperty
        if (JvmCodegenUtil.isConstOrHasJvmFieldAnnotation(property)) {
            return if (descriptor is PropertyGetterDescriptor) {
                substituteGetter(descriptor, expression)
            } else {
                substituteSetter(descriptor, expression)
            }
        } else if (property is SyntheticJavaPropertyDescriptor) {
            expression.dispatchReceiver = expression.extensionReceiver
            expression.extensionReceiver = null
        }
        return super.visitCall(expression)
    }

    private fun substituteSetter(descriptor: PropertyAccessorDescriptor, expression: IrCall): IrSetFieldImpl {
        return IrSetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            context.ir.symbols.externalSymbolTable.referenceField(descriptor.correspondingProperty),
            expression.dispatchReceiver,
            expression.getValueArgument(descriptor.valueParameters.lastIndex)!!,
            expression.type,
            expression.origin,
            expression.superQualifier?.let { context.ir.symbols.externalSymbolTable.referenceClass(it) }
        )
    }

    private fun substituteGetter(descriptor: PropertyGetterDescriptor, expression: IrCall): IrGetFieldImpl {
        return IrGetFieldImpl(
            expression.startOffset,
            expression.endOffset,
            context.ir.symbols.externalSymbolTable.referenceField(descriptor.correspondingProperty),
            expression.type,
            expression.dispatchReceiver,
            expression.origin,
            expression.superQualifier?.let { context.ir.symbols.externalSymbolTable.referenceClass(it) }
        )
    }
}
