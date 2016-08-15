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

package org.jetbrains.kotlin.psi2ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType

abstract class IrDeclarationFactoryBase {
    fun createFunction(ktFunction: KtFunction, functionDescriptor: FunctionDescriptor, body: IrBody): IrFunction =
            IrFunctionImpl(ktFunction.startOffset, ktFunction.endOffset,
                           IrDeclarationOriginKind.DEFINED, functionDescriptor, body)

    fun createSimpleProperty(ktProperty: KtProperty, propertyDescriptor: PropertyDescriptor, valueInitializer: IrBody?): IrSimpleProperty =
            IrSimplePropertyImpl(ktProperty.startOffset, ktProperty.endOffset,
                                 IrDeclarationOriginKind.DEFINED, propertyDescriptor, valueInitializer)

    fun createPropertyGetter(
            ktPropertyAccessor: KtPropertyAccessor,
            irProperty: IrProperty,
            getterDescriptor: PropertyGetterDescriptor,
            getterBody: IrBody
    ): IrPropertyGetter =
            IrPropertyGetterImpl(ktPropertyAccessor.startOffset, ktPropertyAccessor.endOffset,
                                 IrDeclarationOriginKind.DEFINED, getterDescriptor, getterBody)
                    .apply { irProperty.getter = this }

    fun createPropertySetter(
            ktPropertyAccessor: KtPropertyAccessor,
            irProperty: IrProperty,
            setterDescriptor: PropertySetterDescriptor,
            setterBody: IrBody
    ) : IrPropertySetter =
            IrPropertySetterImpl(ktPropertyAccessor.startOffset, ktPropertyAccessor.endOffset,
                                 IrDeclarationOriginKind.DEFINED, setterDescriptor, setterBody)
                    .apply { irProperty.setter = this }
}

class IrDeclarationFactory() : IrDeclarationFactoryBase()

class IrLocalDeclarationsFactory(val scopeOwner: DeclarationDescriptor) : IrDeclarationFactoryBase() {
    private var lastTemporaryIndex = 0
    private fun nextTemporaryIndex() = lastTemporaryIndex++

    fun createDescriptorForTemporaryVariable(type: KotlinType): IrTemporaryVariableDescriptor =
            IrTemporaryVariableDescriptorImpl(scopeOwner, Name.identifier("tmp${nextTemporaryIndex()}"), type)

    fun createTemporaryVariable(ktElement: KtElement, type: KotlinType): IrVariable =
            IrVariableImpl(ktElement.startOffset, ktElement.endOffset,
                           IrDeclarationOriginKind.IR_TEMPORARY_VARIABLE,
                           createDescriptorForTemporaryVariable(type))

    fun createTemporaryVariable(irExpression: IrExpression): IrVariable =
            IrVariableImpl(irExpression.startOffset, irExpression.endOffset,
                           IrDeclarationOriginKind.IR_TEMPORARY_VARIABLE,
                           createDescriptorForTemporaryVariable(
                                        irExpression.type
                                        ?: throw AssertionError("No type for $irExpression")
                                )
            ).apply { initializer = irExpression }

    fun createLocalVariable(ktElement: KtElement, descriptor: VariableDescriptor): IrVariable =
            IrVariableImpl(ktElement.startOffset, ktElement.endOffset,
                           IrDeclarationOriginKind.DEFINED,
                           descriptor)
}
