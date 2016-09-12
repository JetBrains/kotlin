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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.isConstructorDelegatingToSuper
import org.jetbrains.kotlin.resolve.BindingContext

class DeclarationGenerator(override val context: GeneratorContext) : Generator {
    fun generateMemberDeclaration(ktDeclaration: KtDeclaration): IrDeclaration =
            when (ktDeclaration) {
                is KtNamedFunction ->
                    generateFunctionDeclaration(ktDeclaration)
                is KtProperty ->
                    generatePropertyDeclaration(ktDeclaration)
                is KtClassOrObject ->
                    generateClassOrObjectDeclaration(ktDeclaration)
                is KtTypeAlias ->
                    generateTypeAliasDeclaration(ktDeclaration)
                else ->
                    IrErrorDeclarationImpl(
                            ktDeclaration.startOffset, ktDeclaration.endOffset,
                            getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration)
                    )
            }

    fun generateClassMemberDeclaration(ktDeclaration: KtDeclaration, classDescriptor: ClassDescriptor): IrDeclaration =
            when (ktDeclaration) {
                is KtAnonymousInitializer ->
                    generateAnonymousInitializerDeclaration(ktDeclaration, classDescriptor)
                is KtSecondaryConstructor ->
                    generateSecondaryConstructor(ktDeclaration)
                is KtEnumEntry ->
                    generateEnumEntryDeclaration(ktDeclaration)
                else ->
                    generateMemberDeclaration(ktDeclaration)
            }

    private fun generateEnumEntryDeclaration(ktEnumEntry: KtEnumEntry): IrEnumEntry =
            ClassGenerator(this).generateEnumEntry(ktEnumEntry)

    fun generateClassOrObjectDeclaration(ktClassOrObject: KtClassOrObject): IrClass =
            ClassGenerator(this).generateClass(ktClassOrObject)

    fun generateTypeAliasDeclaration(ktDeclaration: KtTypeAlias): IrDeclaration =
            IrTypeAliasImpl(ktDeclaration.startOffset, ktDeclaration.endOffset, IrDeclarationOrigin.DEFINED,
                                                                 getOrFail(BindingContext.TYPE_ALIAS, ktDeclaration))

    fun generateAnonymousInitializerDeclaration(ktAnonymousInitializer: KtAnonymousInitializer, classDescriptor: ClassDescriptor): IrDeclaration {
        val irAnonymousInitializer = IrAnonymousInitializerImpl(ktAnonymousInitializer.startOffset, ktAnonymousInitializer.endOffset,
                                                                                                     IrDeclarationOrigin.DEFINED, classDescriptor)
        irAnonymousInitializer.body = BodyGenerator(classDescriptor, context).generateAnonymousInitializerBody(ktAnonymousInitializer)
        return irAnonymousInitializer
    }

    fun generateFunctionDeclaration(ktFunction: KtNamedFunction): IrFunction {
        val functionDescriptor = getOrFail(BindingContext.FUNCTION, ktFunction)
        val irFunction = IrFunctionImpl(ktFunction.startOffset, ktFunction.endOffset, IrDeclarationOrigin.DEFINED, functionDescriptor)
        val bodyGenerator = createBodyGenerator(functionDescriptor)
        bodyGenerator.generateDefaultParameters(ktFunction, irFunction)
        irFunction.body = ktFunction.bodyExpression?.let { bodyGenerator.generateFunctionBody(it) }
        return irFunction
    }

    fun generateSecondaryConstructor(ktConstructor: KtSecondaryConstructor): IrFunction {
        if (ktConstructor.isConstructorDelegatingToSuper(context.bindingContext)) {
            return generateSecondaryConstructorWithNestedInitializers(ktConstructor)
        }
        val constructorDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor)
        val irConstructor = IrConstructorImpl(ktConstructor.startOffset, ktConstructor.endOffset, IrDeclarationOrigin.DEFINED, constructorDescriptor)
        val bodyGenerator = createBodyGenerator(constructorDescriptor)
        bodyGenerator.generateDefaultParameters(ktConstructor, irConstructor)
        irConstructor.body = bodyGenerator.generateSecondaryConstructorBody(ktConstructor)
        return irConstructor
    }


    private fun generateSecondaryConstructorWithNestedInitializers(ktConstructor: KtSecondaryConstructor): IrFunction {
        val constructorDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor)
        val irConstructor = IrConstructorImpl(ktConstructor.startOffset, ktConstructor.endOffset, IrDeclarationOrigin.DEFINED, constructorDescriptor)
        val bodyGenerator = createBodyGenerator(constructorDescriptor)
        bodyGenerator.generateDefaultParameters(ktConstructor, irConstructor)
        irConstructor.body = createBodyGenerator(constructorDescriptor).generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor)
        return irConstructor
    }

    fun generatePropertyDeclaration(ktProperty: KtProperty): IrProperty {
        val propertyDescriptor = getPropertyDescriptor(ktProperty)
        val ktDelegate = ktProperty.delegate
        return if (ktDelegate != null)
            generateDelegatedProperty(ktProperty, ktDelegate, propertyDescriptor)
        else
            generateSimpleProperty(ktProperty, propertyDescriptor)
    }

    private fun generateDelegatedProperty(ktProperty: KtProperty, ktDelegate: KtPropertyDelegate, propertyDescriptor: PropertyDescriptor): IrProperty {
        val ktDelegateExpression = ktDelegate.expression!!
        val irDelegateInitializer = generateInitializerBody(propertyDescriptor, ktDelegateExpression)
        return DelegatedPropertyGenerator(context).generateDelegatedProperty(ktProperty, ktDelegate, propertyDescriptor, irDelegateInitializer)
    }

    private fun generateSimpleProperty(ktProperty: KtProperty, propertyDescriptor: PropertyDescriptor): IrProperty {
        val irProperty = IrPropertyImpl(ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOrigin.DEFINED, false, propertyDescriptor)

        val irField = if (propertyDescriptor.hasBackingField()) {
            IrFieldImpl(ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOrigin.PROPERTY_BACKING_FIELD, propertyDescriptor,
                        ktProperty.initializer?.let { generateInitializerBody(propertyDescriptor, it) })
        }
        else null
        irProperty.backingField = irField

        irProperty.getter = ktProperty.getter?.let { ktGetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktGetter)
            val getterDescriptor = accessorDescriptor as? PropertyGetterDescriptor ?: TODO("not a getter?")
            val irGetter = IrFunctionImpl(ktGetter.startOffset, ktGetter.endOffset, IrDeclarationOrigin.DEFINED, getterDescriptor)
            irGetter.body = ktGetter.bodyExpression?.let { generateFunctionBody(getterDescriptor, it ) }
            irGetter
        } ?: generateDefaultGetterIfRequired(ktProperty, irField)

        irProperty.setter = ktProperty.setter?.let { ktSetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktSetter)
            val setterDescriptor = accessorDescriptor as? PropertySetterDescriptor ?: TODO("not a setter?")
            val irSetter = IrFunctionImpl(ktSetter.startOffset, ktSetter.endOffset, IrDeclarationOrigin.DEFINED, setterDescriptor)
            irSetter.body = ktSetter.bodyExpression?.let { generateFunctionBody(setterDescriptor, it ) }
            irSetter
        } ?: generateDefaultSetterIfRequired(ktProperty, irField)

        return irProperty
    }

    private fun PropertyDescriptor.hasBackingField(): Boolean =
            get(BindingContext.BACKING_FIELD_REQUIRED, this) ?: false

    private fun generateDefaultGetterIfRequired(ktProperty: KtProperty, irField: IrField?): IrFunction? {
        if (irField == null) return null

        val property = irField.descriptor
        val getter = property.getter ?: return null

        val irGetter = IrFunctionImpl(ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR, getter)
        val irBody = IrBlockBodyImpl(ktProperty.startOffset, ktProperty.endOffset)
        irGetter.body = irBody

        val containingDeclaration = property.containingDeclaration
        val receiver =
                if (containingDeclaration is ClassDescriptor)
                    IrThisReferenceImpl(ktProperty.startOffset, ktProperty.endOffset, containingDeclaration.defaultType,
                                        containingDeclaration)
                else
                    null

        irBody.addStatement(IrReturnImpl(ktProperty.startOffset, ktProperty.endOffset, context.builtIns.nothingType, getter,
                                         IrGetFieldImpl(ktProperty.startOffset, ktProperty.endOffset, property, receiver)))

        return irGetter
    }

    private fun generateDefaultSetterIfRequired(ktProperty: KtProperty, irField: IrField?): IrFunction? {
        if (irField == null) return null

        val property = irField.descriptor
        if (!property.isVar) return null
        val setter = property.setter ?: return null

        val irSetter = IrFunctionImpl(ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR, setter)
        val irBody = IrBlockBodyImpl(ktProperty.startOffset, ktProperty.endOffset)
        irSetter.body = irBody

        val containingDeclaration = property.containingDeclaration
        val receiver =
                if (containingDeclaration is ClassDescriptor)
                    IrThisReferenceImpl(ktProperty.startOffset, ktProperty.endOffset, containingDeclaration.defaultType,
                                        containingDeclaration)
                else
                    null

        val setterParameter = setter.valueParameters.single()
        irBody.addStatement(IrSetFieldImpl(ktProperty.startOffset, ktProperty.endOffset, property, receiver,
                                           IrGetVariableImpl(ktProperty.startOffset, ktProperty.endOffset, setterParameter)))

        return irSetter
    }

    private fun getPropertyDescriptor(ktProperty: KtProperty): PropertyDescriptor {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)
        val propertyDescriptor = variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
        return propertyDescriptor
    }

    private fun generateFunctionBody(scopeOwner: CallableDescriptor, ktBody: KtExpression): IrBody =
            createBodyGenerator(scopeOwner).generateFunctionBody(ktBody)

    private fun generateInitializerBody(scopeOwner: CallableDescriptor, ktBody: KtExpression): IrExpressionBody =
            createBodyGenerator(scopeOwner).generatePropertyInitializerBody(ktBody)

    private fun createBodyGenerator(descriptor: CallableDescriptor) =
            BodyGenerator(descriptor, context)

}