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

import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.pureEndOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.pureStartOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.hasBackingField

internal class PropertyGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generatePropertyDeclaration(ktProperty: KtProperty): IrProperty {
        val propertyDescriptor = getPropertyDescriptor(ktProperty)
        val ktDelegate = ktProperty.delegate
        return if (ktDelegate != null)
            generateDelegatedProperty(ktProperty, ktDelegate, propertyDescriptor)
        else
            generateSimpleProperty(ktProperty, propertyDescriptor)
    }

    fun generateDestructuringDeclarationEntryAsPropertyDeclaration(ktEntry: KtDestructuringDeclarationEntry): IrProperty {
        val propertyDescriptor = getPropertyDescriptor(ktEntry)
        return context.symbolTable.descriptorExtension.declareProperty(
            ktEntry.startOffsetSkippingComments, ktEntry.endOffset,
            IrDeclarationOrigin.DEFINED,
            propertyDescriptor,
            isDelegated = false
        ).buildWithScope { irProperty ->
            irProperty.backingField = generatePropertyBackingField(ktEntry, propertyDescriptor) { null }

            irProperty.getter = generateGetterIfRequired(ktEntry, propertyDescriptor)
            irProperty.setter = generateSetterIfRequired(ktEntry, propertyDescriptor)

            irProperty.linkCorrespondingPropertySymbol()
            irProperty.generateOverrides(propertyDescriptor)
        }
    }

    fun generatePropertyForPrimaryConstructorParameter(ktParameter: KtParameter, irValueParameter: IrValueParameter): IrDeclaration {
        val propertyDescriptor = getOrFail(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, ktParameter)
        return generateSyntheticProperty(ktParameter, propertyDescriptor, irValueParameter)
    }

    fun generateSyntheticProperty(
        ktDeclarationContainer: KtElement,
        propertyDescriptor: PropertyDescriptor,
        irValueParameter: IrValueParameter?,
        generateSyntheticAccessors: Boolean = false
    ): IrProperty {
        val irPropertyType = propertyDescriptor.type.toIrType()
        return generateSyntheticPropertyWithInitializer(ktDeclarationContainer, propertyDescriptor, generateSyntheticAccessors) {
            if (irValueParameter == null)
                null
            else if (context.configuration.skipBodies && !DescriptorUtils.isAnnotationClass(propertyDescriptor.containingDeclaration))
                null
            else
                context.irFactory.createExpressionBody(
                    IrGetValueImpl(
                        ktDeclarationContainer.startOffsetSkippingComments, ktDeclarationContainer.endOffset,
                        irPropertyType,
                        irValueParameter.symbol,
                        IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
                    )
                )
        }
    }

    fun generateSyntheticPropertyWithInitializer(
        ktDeclarationContainer: KtElement,
        propertyDescriptor: PropertyDescriptor,
        generateSyntheticAccessors: Boolean,
        generateInitializer: (IrField) -> IrExpressionBody?
    ): IrProperty {
        return context.symbolTable.descriptorExtension.declareProperty(
            ktDeclarationContainer.startOffsetSkippingComments, ktDeclarationContainer.endOffset,
            IrDeclarationOrigin.DEFINED,
            propertyDescriptor,
            isDelegated = false
        ).also { irProperty ->
            irProperty.backingField = generatePropertyBackingField(ktDeclarationContainer, propertyDescriptor, generateInitializer)

            val getter = propertyDescriptor.getter
                ?: if (generateSyntheticAccessors) {
                    PropertyGetterDescriptorImpl(
                        propertyDescriptor,
                        Annotations.EMPTY, Modality.FINAL, DescriptorVisibilities.PUBLIC, false, false, false,
                        CallableMemberDescriptor.Kind.SYNTHESIZED, null, propertyDescriptor.source
                    ).apply {
                        initialize(propertyDescriptor.type)
                    }
                } else
                    throw AssertionError("Property declared in primary constructor has no getter: $propertyDescriptor")
            irProperty.getter =
                FunctionGenerator(declarationGenerator).generateDefaultAccessorForPrimaryConstructorParameter(
                    getter,
                    ktDeclarationContainer
                )

            if (propertyDescriptor.isVar) {
                val setter = propertyDescriptor.setter
                    ?: if (generateSyntheticAccessors) {
                        PropertySetterDescriptorImpl(
                            propertyDescriptor,
                            Annotations.EMPTY, Modality.FINAL, DescriptorVisibilities.PUBLIC, false, false, false,
                            CallableMemberDescriptor.Kind.SYNTHESIZED, null, propertyDescriptor.source
                        ).apply {
                            val setterValueParameter = ValueParameterDescriptorImpl(
                                this, null, 0, Annotations.EMPTY, DEFAULT_VALUE_PARAMETER, propertyDescriptor.type,
                                declaresDefaultValue = false, isCrossinline = false, isNoinline = false,
                                varargElementType = null, source = SourceElement.NO_SOURCE
                            )
                            initialize(setterValueParameter)
                        }
                    } else
                        throw AssertionError("Property declared in primary constructor has no setter: $propertyDescriptor")
                irProperty.setter =
                    FunctionGenerator(declarationGenerator).generateDefaultAccessorForPrimaryConstructorParameter(
                        setter,
                        ktDeclarationContainer
                    )
            }

            irProperty.linkCorrespondingPropertySymbol()
            irProperty.generateOverrides(propertyDescriptor)
        }
    }

    private inline fun generatePropertyBackingField(
        ktPropertyElement: KtElement,
        propertyDescriptor: PropertyDescriptor,
        generateInitializer: (IrField) -> IrExpressionBody?
    ): IrField =
        context.symbolTable.descriptorExtension.declareField(
            ktPropertyElement.startOffsetSkippingComments, ktPropertyElement.endOffset,
            IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
            propertyDescriptor, propertyDescriptor.type.toIrType(),
            propertyDescriptor.fieldVisibility
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
            .apply { generateOverrides(propertyDescriptor) }

    private fun PropertyDescriptor.actuallyHasBackingField(bindingContext: BindingContext) =
        hasBackingField(bindingContext) || context.extensions.isPropertyWithPlatformField(this)

    private fun generateSimpleProperty(ktProperty: KtVariableDeclaration, propertyDescriptor: PropertyDescriptor): IrProperty =
        context.symbolTable.descriptorExtension.declareProperty(
            ktProperty.startOffsetSkippingComments, ktProperty.endOffset,
            IrDeclarationOrigin.DEFINED,
            propertyDescriptor,
            isDelegated = false
        ).buildWithScope { irProperty ->
            irProperty.backingField =
                if (propertyDescriptor.actuallyHasBackingField(context.bindingContext))
                    generatePropertyBackingField(ktProperty, propertyDescriptor) { irField ->
                        ktProperty.initializer?.let evaluateInitializer@{ ktInitializer ->
                            val compileTimeConst = propertyDescriptor.compileTimeInitializer
                            if (compileTimeConst != null) {
                                val constantInfo = context.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, ktInitializer)
                                if (propertyDescriptor.isConst ||
                                    (constantInfo?.usesNonConstValAsConstant == false &&
                                            (!constantInfo.usesVariableAsConstant ||
                                                    context.languageVersionSettings.supportsFeature(LanguageFeature.InlineConstVals)))
                                ) {
                                    return@evaluateInitializer context.irFactory.createExpressionBody(
                                        context.constantValueGenerator.generateConstantValueAsExpression(
                                            ktInitializer.startOffsetSkippingComments, ktInitializer.endOffset,
                                            compileTimeConst
                                        )
                                    )
                                }
                            }

                            if (context.configuration.skipBodies) null
                            else declarationGenerator.generateInitializerBody(irField.symbol, ktInitializer)
                        }
                    }
                else
                    null

            irProperty.getter = generateGetterIfRequired(ktProperty, propertyDescriptor)
            irProperty.setter = generateSetterIfRequired(ktProperty, propertyDescriptor)

            irProperty.linkCorrespondingPropertySymbol()
            irProperty.generateOverrides(propertyDescriptor)
        }

    fun generateFakeOverrideProperty(propertyDescriptor: PropertyDescriptor, ktElement: KtPureElement): IrProperty? {
        if (propertyDescriptor.visibility == DescriptorVisibilities.INVISIBLE_FAKE) return null

        val startOffset = ktElement.pureStartOffsetOrUndefined
        val endOffset = ktElement.pureEndOffsetOrUndefined

        return context.symbolTable.descriptorExtension.declareProperty(startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, propertyDescriptor, propertyDescriptor.isDelegated).apply {
            this.getter = propertyDescriptor.getter?.let {
                FunctionGenerator(declarationGenerator).generateFakeOverrideFunction(it, ktElement)
            }
            this.setter = propertyDescriptor.setter?.let {
                FunctionGenerator(declarationGenerator).generateFakeOverrideFunction(it, ktElement)
            }
            this.linkCorrespondingPropertySymbol()
            this.generateOverrides(propertyDescriptor)
        }
    }

    private fun IrProperty.generateOverrides(propertyDescriptor: PropertyDescriptor) {
        overriddenSymbols =
            propertyDescriptor.overriddenDescriptors.map { overriddenPropertyDescriptor ->
                context.symbolTable.descriptorExtension.referenceProperty(overriddenPropertyDescriptor.original)
            }
    }

    private fun generateGetterIfRequired(ktProperty: KtVariableDeclaration, property: PropertyDescriptor): IrSimpleFunction? {
        val getter = property.getter ?: return null
        return FunctionGenerator(declarationGenerator).generatePropertyAccessor(getter, ktProperty, (ktProperty as? KtProperty)?.getter)
    }

    private fun generateSetterIfRequired(ktProperty: KtVariableDeclaration, property: PropertyDescriptor): IrSimpleFunction? {
        if (!property.isVar) return null
        val setter = property.setter ?: return null
        return FunctionGenerator(declarationGenerator).generatePropertyAccessor(setter, ktProperty, (ktProperty as? KtProperty)?.setter)
    }

    private fun getPropertyDescriptor(ktProperty: KtVariableDeclaration): PropertyDescriptor {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)
        return variableDescriptor as? PropertyDescriptor ?: TODO("not a property: $variableDescriptor")
    }

    private val PropertyDescriptor.fieldVisibility: DescriptorVisibility
        get() = declarationGenerator.context.extensions.computeFieldVisibility(this)
            ?: when {
                isLateInit -> setter?.visibility ?: visibility
                isConst -> visibility
                else -> DescriptorVisibilities.PRIVATE
            }
}

internal fun IrProperty.linkCorrespondingPropertySymbol() {
    backingField?.correspondingPropertySymbol = symbol
    getter?.correspondingPropertySymbol = symbol
    setter?.correspondingPropertySymbol = symbol
}
