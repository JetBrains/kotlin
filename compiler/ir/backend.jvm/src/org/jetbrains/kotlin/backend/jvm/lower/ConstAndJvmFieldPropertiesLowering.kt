/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class ConstAndJvmFieldPropertiesLowering : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        return super.visitDeclaration(declaration)
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (JvmCodegenUtil.isConstOrHasJvmFieldAnnotation(declaration.descriptor)) {
            /*Safe or need copy?*/
            declaration.getter = null
            declaration.setter = null
        }
        return super.visitProperty(declaration)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression {
        return super.visitMemberAccess(expression)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val descriptor = expression.descriptor
        if (descriptor !is PropertyAccessorDescriptor) {
            return super.visitCall(expression)
        }

        val property = descriptor.correspondingProperty
        if (JvmCodegenUtil.isConstOrHasJvmFieldAnnotation(property)) {
            return if (descriptor is PropertyGetterDescriptor) {
                substituteGetter(descriptor, expression)
            }
            else {
                substituteSetter(descriptor, expression)
            }
        }
        else if (property is SyntheticJavaPropertyDescriptor) {
            expression.dispatchReceiver = expression.extensionReceiver
            expression.extensionReceiver = null
        }
        return super.visitCall(expression)
    }

    private fun substituteSetter(descriptor: PropertyAccessorDescriptor, expression: IrCall): IrSetFieldImpl {
        return IrSetFieldImpl(
                expression.startOffset,
                expression.endOffset,
                descriptor.correspondingProperty,
                expression.dispatchReceiver,
                expression.getValueArgument(descriptor.valueParameters.lastIndex)!!,
                expression.origin,
                expression.superQualifier
        )
    }

    private fun substituteGetter(descriptor: PropertyGetterDescriptor, expression: IrCall): IrGetFieldImpl {
        return IrGetFieldImpl(
                expression.startOffset,
                expression.endOffset,
                descriptor.correspondingProperty,
                expression.dispatchReceiver,
                expression.origin,
                expression.superQualifier
        )
    }
}
