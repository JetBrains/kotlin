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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.isCompanionObjectInInterfaceNotIntrinsic

class ObjectClassLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    private var pendingTransformations = mutableListOf<Function0<Unit>>()

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)

        pendingTransformations.forEach { it() }
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        process(declaration)
        return super.visitClassNew(declaration)
    }


    private fun process(irClass: IrClass) {
        if (irClass.descriptor.kind != ClassKind.OBJECT) return

        val publicInstanceDescriptor = context.descriptorsFactory.getFieldDescriptorForObjectInstance(irClass.descriptor)

        val constructor = irClass.descriptor.unsubstitutedPrimaryConstructor ?:
                          throw AssertionError("Object should have a primary constructor: ${irClass.descriptor}")

        val publicInstanceOwner = if (irClass.descriptor.isCompanionObject) parentScope!!.irElement as IrDeclarationContainer else irClass
        if (isCompanionObjectInInterfaceNotIntrinsic(irClass.descriptor)) {
            // TODO rename to $$INSTANCE
            val privateInstance = publicInstanceDescriptor.copy(irClass.descriptor, Modality.FINAL, Visibilities.PROTECTED/*TODO package local*/, CallableMemberDescriptor.Kind.SYNTHESIZED, false) as PropertyDescriptor
            privateInstance.name
            val field = createInstanceFieldWithInitializer(privateInstance, constructor, irClass)
            createFieldWithCustomInitializer(
                    publicInstanceDescriptor,
                    IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol),
                    publicInstanceOwner
            )
        } else {
            createInstanceFieldWithInitializer(publicInstanceDescriptor, constructor, publicInstanceOwner)
        }
    }

    private fun createInstanceFieldWithInitializer(
            instanceFieldDescriptor: PropertyDescriptor,
            constructor: ClassConstructorDescriptor,
            instanceOwner: IrDeclarationContainer
    ): IrField =
            createFieldWithCustomInitializer(instanceFieldDescriptor, IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, constructor), instanceOwner)

    private fun createFieldWithCustomInitializer(
            instanceFieldDescriptor: PropertyDescriptor,
            instanceInitializer: IrExpression,
            instanceOwner: IrDeclarationContainer
    ): IrField =
            IrFieldImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE,
                    instanceFieldDescriptor,
                    IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, instanceInitializer)
            ).also {
                pendingTransformations.add { instanceOwner.declarations.add(it) }
            }
}