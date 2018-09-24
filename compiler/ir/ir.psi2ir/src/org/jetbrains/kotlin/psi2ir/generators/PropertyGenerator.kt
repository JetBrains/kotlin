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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.declareFieldWithOverrides
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.pureEndOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.pureStartOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

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

        val irPropertyType = propertyDescriptor.type.toIrType()
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
                                irPropertyType,
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
            propertyDescriptor, propertyDescriptor.type.toIrType()
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
            IrDeclarationOrigin.DEFINED,
            false,
            propertyDescriptor
        ).buildWithScope { irProperty ->
            irProperty.backingField =
                    if (propertyDescriptor.hasBackingField(context.bindingContext))
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

    fun generateFakeOverrideProperty(propertyDescriptor: PropertyDescriptor, ktElement: KtPureElement): IrProperty {
        val startOffset = ktElement.pureStartOffsetOrUndefined
        val endOffset = ktElement.pureEndOffsetOrUndefined

        val backingField =
            if (propertyDescriptor.hasBackingField(context.bindingContext))
                context.symbolTable.declareFieldWithOverrides(
                    startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE,
                    propertyDescriptor, propertyDescriptor.type.toIrType(),
                    { it.hasBackingField(context.bindingContext) }
                )
            else
                null

        return IrPropertyImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.FAKE_OVERRIDE,
            false,
            propertyDescriptor,
            backingField,
            propertyDescriptor.getter?.let { FunctionGenerator(declarationGenerator).generateFakeOverrideFunction(it, ktElement) },
            propertyDescriptor.setter?.let { FunctionGenerator(declarationGenerator).generateFakeOverrideFunction(it, ktElement) }
        )
    }

    private fun generateGetterIfRequired(ktProperty: KtProperty, property: PropertyDescriptor): IrSimpleFunction? {
        val getter = property.getter ?: return null
        return FunctionGenerator(declarationGenerator).generatePropertyAccessor(getter, ktProperty, ktProperty.getter)
    }

    private fun generateSetterIfRequired(ktProperty: KtProperty, property: PropertyDescriptor): IrSimpleFunction? {
        if (!property.isVar) return null
        val setter = property.setter ?: return null
        return FunctionGenerator(declarationGenerator).generatePropertyAccessor(setter, ktProperty, ktProperty.setter)
    }

    private fun getPropertyDescriptor(ktProperty: KtProperty): PropertyDescriptor {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)
        return variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
    }
}

