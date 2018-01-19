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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class PropertyGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generatePropertyDeclaration(ktProperty: KtProperty): IrProperty {
        val propertyDescriptor = getPropertyDescriptor(ktProperty)
        val ktDelegate = ktProperty.delegate
        return if (ktDelegate != null)
            generateDelegatedProperty(ktProperty, ktDelegate, propertyDescriptor)
        else
            generateSimpleProperty(ktProperty, propertyDescriptor)
    }

    fun generatePropertyForPrimaryConstructorParameter(ktParameter: KtParameter, irValueParameter: IrValueParameter): IrDeclaration {
        val propertyDescriptor = getOrFail(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, ktParameter)

        return IrPropertyImpl(
            ktParameter.startOffset, ktParameter.endOffset,
            IrDeclarationOrigin.DEFINED, false,
            propertyDescriptor
        ).also { irProperty ->
            irProperty.backingField =
                    generatePropertyBackingField(ktParameter, propertyDescriptor) {
                        IrExpressionBodyImpl(
                            IrGetValueImpl(
                                ktParameter.startOffset, ktParameter.endOffset,
                                irValueParameter.symbol,
                                IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
                            )
                        )
                    }

            val getter = propertyDescriptor.getter
                    ?: throw AssertionError("Property declared in primary constructor has no getter: $propertyDescriptor")
            irProperty.getter =
                    FunctionGenerator(declarationGenerator).generateDefaultAccessorForPrimaryConstructorParameter(getter, ktParameter)

            if (propertyDescriptor.isVar) {
                val setter = propertyDescriptor.setter
                        ?: throw AssertionError("Property declared in primary constructor has no setter: $propertyDescriptor")
                irProperty.setter =
                        FunctionGenerator(declarationGenerator).generateDefaultAccessorForPrimaryConstructorParameter(setter, ktParameter)
            }
        }
    }

    private inline fun generatePropertyBackingField(
        ktPropertyElement: KtElement,
        propertyDescriptor: PropertyDescriptor,
        generateInitializer: (IrField) -> IrExpressionBody?
    ): IrField =
        context.symbolTable.declareField(
            ktPropertyElement.startOffset, ktPropertyElement.endOffset,
            IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
            propertyDescriptor
        ).also {
            it.initializer = generateInitializer(it)
        }


    private fun generateDelegatedProperty(
        ktProperty: KtProperty,
        ktDelegate: KtPropertyDelegate,
        propertyDescriptor: PropertyDescriptor
    ): IrProperty =
        DelegatedPropertyGenerator(declarationGenerator)
            .generateDelegatedProperty(ktProperty, ktDelegate, propertyDescriptor)

    private fun generateSimpleProperty(ktProperty: KtProperty, propertyDescriptor: PropertyDescriptor): IrProperty =
        IrPropertyImpl(
            ktProperty.startOffset, ktProperty.endOffset,
            IrDeclarationOrigin.DEFINED, false,
            propertyDescriptor
        ).buildWithScope { irProperty ->
            irProperty.backingField =
                    if (propertyDescriptor.hasBackingField())
                        generatePropertyBackingField(ktProperty, propertyDescriptor) { irField ->
                            ktProperty.initializer?.let { ktInitializer ->
                                declarationGenerator.generateInitializerBody(irField.symbol, ktInitializer)
                            }
                        }
                    else
                        null

            irProperty.getter = generateGetterIfRequired(ktProperty, propertyDescriptor)

            irProperty.setter = generateSetterIfRequired(ktProperty, propertyDescriptor)
        }

    private fun PropertyDescriptor.hasBackingField(): Boolean =
        get(BindingContext.BACKING_FIELD_REQUIRED, this) ?: false

    private fun generateGetterIfRequired(ktProperty: KtProperty, property: PropertyDescriptor): IrFunction? {
        val getter = property.getter ?: return null
        return FunctionGenerator(declarationGenerator).generatePropertyAccessor(getter, ktProperty, ktProperty.getter)
    }

    private fun generateSetterIfRequired(ktProperty: KtProperty, property: PropertyDescriptor): IrFunction? {
        if (!property.isVar) return null
        val setter = property.setter ?: return null
        return FunctionGenerator(declarationGenerator).generatePropertyAccessor(setter, ktProperty, ktProperty.setter)
    }

    private fun getPropertyDescriptor(ktProperty: KtProperty): PropertyDescriptor {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)
        return variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
    }
}
