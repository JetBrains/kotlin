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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class DeclarationGenerator(override val context: GeneratorContext) : Generator {
    fun generateMemberDeclaration(ktDeclaration: KtDeclaration): IrDeclaration =
            when (ktDeclaration) {
                is KtNamedFunction ->
                    generateFunctionDeclaration(ktDeclaration)
                is KtSecondaryConstructor ->
                    generateSecondaryConstructor(ktDeclaration)
                is KtProperty ->
                    generatePropertyDeclaration(ktDeclaration)
                is KtClassOrObject ->
                    generateClassOrObjectDeclaration(ktDeclaration)
                is KtTypeAlias ->
                    generateTypeAliasDeclaration(ktDeclaration)
                else ->
                    IrDummyDeclaration(
                            ktDeclaration.startOffset, ktDeclaration.endOffset,
                            getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration)
                    )
            }

    fun generateClassOrObjectDeclaration(ktClassOrObject: KtClassOrObject): IrClass =
            ClassGenerator(this).generateClass(ktClassOrObject)

    fun generateTypeAliasDeclaration(ktDeclaration: KtTypeAlias): IrDeclaration =
            IrTypeAliasImpl(ktDeclaration.startOffset, ktDeclaration.endOffset, IrDeclarationOrigin.DEFINED,
                            getOrFail(BindingContext.TYPE_ALIAS, ktDeclaration))

    fun generateFunctionDeclaration(ktFunction: KtNamedFunction): IrFunction {
        val functionDescriptor = getOrFail(BindingContext.FUNCTION, ktFunction)
        val body = ktFunction.bodyExpression?.let { generateFunctionBody(functionDescriptor, it) }
        return IrFunctionImpl(ktFunction.startOffset, ktFunction.endOffset, IrDeclarationOrigin.DEFINED,
                              functionDescriptor, body)
    }

    fun generateSecondaryConstructor(ktConstructor: KtSecondaryConstructor) : IrFunction {
        val constructorDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor)
        val body = BodyGenerator(constructorDescriptor, context).generateSecondaryConstructorBody(ktConstructor)
        return IrFunctionImpl(ktConstructor.startOffset, ktConstructor.endOffset, IrDeclarationOrigin.DEFINED,
                              constructorDescriptor, body)
    }

    fun generatePropertyDeclaration(ktProperty: KtProperty): IrProperty {
        val propertyDescriptor = getPropertyDescriptor(ktProperty)
        if (ktProperty.hasDelegate()) TODO("handle delegated property")
        val initializer = ktProperty.initializer?.let { generateInitializerBody(propertyDescriptor, it) }
        val irProperty = IrSimplePropertyImpl(ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOrigin.DEFINED,
                                              propertyDescriptor, initializer)

        ktProperty.getter?.let { ktGetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktGetter)
            val getterDescriptor = accessorDescriptor as? PropertyGetterDescriptor ?: TODO("not a getter?")
            val irGetterBody = generateFunctionBody(getterDescriptor, ktGetter.bodyExpression ?: TODO("default getter"))
            val irGetter = IrPropertyGetterImpl(ktGetter.startOffset, ktGetter.endOffset, IrDeclarationOrigin.DEFINED,
                                                getterDescriptor, irGetterBody)
            irProperty.getter = irGetter
        }

        ktProperty.setter?.let { ktSetter ->
            val accessorDescriptor = getOrFail(BindingContext.PROPERTY_ACCESSOR, ktSetter)
            val setterDescriptor = accessorDescriptor as? PropertySetterDescriptor ?: TODO("not a setter?")
            val irSetterBody = generateFunctionBody(setterDescriptor, ktSetter.bodyExpression ?: TODO("default setter"))
            val irSetter = IrPropertySetterImpl(ktSetter.startOffset, ktSetter.endOffset, IrDeclarationOrigin.DEFINED,
                                                setterDescriptor, irSetterBody)
            irProperty.setter = irSetter
        }

        return irProperty
    }


    private fun getPropertyDescriptor(ktProperty: KtProperty): PropertyDescriptor {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty)
        val propertyDescriptor = variableDescriptor as? PropertyDescriptor ?: TODO("not a property?")
        return propertyDescriptor
    }

    private fun generateFunctionBody(scopeOwner: CallableDescriptor, ktBody: KtExpression): IrBody =
            BodyGenerator(scopeOwner, context).generateFunctionBody(ktBody)

    private fun generateInitializerBody(scopeOwner: CallableDescriptor, ktBody: KtExpression): IrBody =
            BodyGenerator(scopeOwner, context).generatePropertyInitializerBody(ktBody)
}