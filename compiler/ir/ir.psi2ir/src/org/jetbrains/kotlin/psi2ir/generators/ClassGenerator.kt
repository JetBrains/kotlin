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

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class ClassGenerator(val declarationGenerator: DeclarationGenerator) : Generator {
    override val context: GeneratorContext get() = declarationGenerator.context

    fun generateClass(ktClassOrObject: KtClassOrObject): IrClass {
        val descriptor = getOrFail(BindingContext.CLASS, ktClassOrObject)
        val irClass = IrClassImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset, IrDeclarationOrigin.DEFINED, descriptor)

        generatePrimaryConstructor(irClass, ktClassOrObject)
        generatePropertiesDeclaredInPrimaryConstructor(irClass, ktClassOrObject)
        generateMembersDeclaredInClassBody(irClass, ktClassOrObject)

        return irClass
    }

    private fun generatePrimaryConstructor(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        val ktPrimaryConstructor = ktClassOrObject.getPrimaryConstructor() ?: return

        val primaryConstructorDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktPrimaryConstructor)
        val irPrimaryConstructor = IrFunctionImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset, IrDeclarationOrigin.DEFINED,
                                                  primaryConstructorDescriptor)

        irPrimaryConstructor.body = generatePrimaryConstructorBodyFromClass(ktClassOrObject, primaryConstructorDescriptor)

        irClass.addMember(irPrimaryConstructor)
    }

    private fun generatePropertiesDeclaredInPrimaryConstructor(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getPrimaryConstructor()?.let { ktPrimaryConstructor ->
            for (ktParameter in ktPrimaryConstructor.valueParameters) {
                if (ktParameter.hasValOrVar()) {
                    irClass.addMember(generatePropertyForPrimaryConstructorParameter(ktParameter))
                }
            }
        }
    }

    private fun generateMembersDeclaredInClassBody(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getBody()?.let { ktClassBody ->
            for (ktDeclaration in ktClassBody.declarations) {
                if (ktDeclaration is KtAnonymousInitializer) continue

                val irMember = declarationGenerator.generateMemberDeclaration(ktDeclaration)
                irClass.addMember(irMember)
                if (irMember is IrProperty) {
                    irMember.getter?.let { irClass.addMember(it) }
                    irMember.setter?.let { irClass.addMember(it) }
                }
            }
        }
    }

    private fun generatePropertyForPrimaryConstructorParameter(ktParameter: KtParameter): IrDeclaration {
        val valueParameterDescriptor = getOrFail(BindingContext.VALUE_PARAMETER, ktParameter)
        val propertyDescriptor = getOrFail(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, ktParameter)
        val irProperty = IrSimplePropertyImpl(ktParameter.startOffset, ktParameter.endOffset, IrDeclarationOrigin.DEFINED, propertyDescriptor)
        val irGetParameter = IrGetVariableImpl(ktParameter.startOffset, ktParameter.endOffset,
                                               valueParameterDescriptor, IrOperator.INITIALIZE_PROPERTY_FROM_PARAMETER)
        irProperty.valueInitializer = IrExpressionBodyImpl(ktParameter.startOffset, ktParameter.endOffset, irGetParameter)
        return irProperty
    }

    private fun generatePrimaryConstructorBodyFromClass(ktClassOrObject: KtClassOrObject, primaryConstructorDescriptor: ConstructorDescriptor): IrBody {
        val irBlockBody = IrBlockBodyImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset)

        generateInitializersForPropertiesDefinedInPrimaryConstructor(irBlockBody, ktClassOrObject)
        generateInitializersForClassBody(irBlockBody, ktClassOrObject, primaryConstructorDescriptor)

        return irBlockBody
    }

    private fun generateInitializersForClassBody(irBlockBody: IrBlockBodyImpl, ktClassOrObject: KtClassOrObject, primaryConstructorDescriptor: ConstructorDescriptor) {
        ktClassOrObject.getBody()?.let { ktClassBody ->
            for (ktDeclaration in ktClassBody.declarations) {
                when (ktDeclaration) {
                    is KtProperty -> generateInitializerForPropertyDefinedInClassBody(irBlockBody, ktDeclaration)
                    is KtClassInitializer -> generateAnonymousInitializer(irBlockBody, ktDeclaration, primaryConstructorDescriptor)
                }
            }
        }
    }

    private fun generateAnonymousInitializer(irBlockBody: IrBlockBodyImpl, ktClassInitializer: KtClassInitializer, primaryConstructorDescriptor: ConstructorDescriptor) {
        if (ktClassInitializer.body == null) return
        val irInitializer = BodyGenerator(primaryConstructorDescriptor, context).generateAnonymousInitializer(ktClassInitializer)
        irBlockBody.addStatement(irInitializer)
    }

    private fun generateInitializerForPropertyDefinedInClassBody(irBlockBody: IrBlockBodyImpl, ktProperty: KtProperty) {
        val propertyDescriptor = getOrFail(BindingContext.VARIABLE, ktProperty) as PropertyDescriptor
        if (ktProperty.initializer != null) {
            irBlockBody.addStatement(createInitializeProperty(ktProperty, propertyDescriptor))
        }
    }

    private fun generateInitializersForPropertiesDefinedInPrimaryConstructor(irBlockBody: IrBlockBodyImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getPrimaryConstructor()?.let { ktPrimaryConstructor ->
            for (ktParameter in ktPrimaryConstructor.valueParameters) {
                if (ktParameter.hasValOrVar()) {
                    val propertyDescriptor = getOrFail(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, ktParameter)
                    irBlockBody.addStatement(createInitializeProperty(ktParameter, propertyDescriptor))
                }
            }
        }
    }

    private fun createInitializeProperty(ktElement: KtElement, propertyDescriptor: PropertyDescriptor) =
            IrInitializePropertyImpl(ktElement.startOffset, ktElement.endOffset, context.builtIns.unitType, propertyDescriptor)


}
