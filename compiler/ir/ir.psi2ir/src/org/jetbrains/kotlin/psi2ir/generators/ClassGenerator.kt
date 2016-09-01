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

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.IrGetVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter
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

        generateMembersDeclaredInSupertypeList(irClass, ktClassOrObject)

        generateMembersDeclaredInClassBody(irClass, ktClassOrObject)

        if (descriptor.isData) {
            generateAdditionalMembersForDataClass(irClass, ktClassOrObject)
        }

        if (descriptor.kind == ClassKind.ENUM_CLASS) {
            generateAdditionalMembersForEnumClass(irClass)
        }

        return irClass
    }

    private fun generateMembersDeclaredInSupertypeList(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getSuperTypeList()?.let { ktSuperTypeList ->
            for (ktEntry in ktSuperTypeList.entries) {
                if (ktEntry is KtDelegatedSuperTypeEntry) {
                    generateDelegatedImplementationMembers(irClass, ktEntry)
                }
            }
        }
    }

    private fun generateDelegatedImplementationMembers(irClass: IrClassImpl, ktEntry: KtDelegatedSuperTypeEntry) {
        val ktDelegateExpression = ktEntry.delegateExpression!!
        val delegateType = getInferredTypeWithImplicitCastsOrFail(ktDelegateExpression)
        val superType = getOrFail(BindingContext.TYPE, ktEntry.typeReference!!)
        val delegateDescriptor = IrImplementingDelegateDescriptorImpl(irClass.descriptor, delegateType, superType)
        val irDelegate = IrDelegateImpl(ktDelegateExpression.startOffset, ktDelegateExpression.endOffset, IrDeclarationOrigin.DELEGATE,
                                        delegateDescriptor)
        val bodyGenerator = BodyGenerator(irClass.descriptor, context)
        irDelegate.initializer = bodyGenerator.generatePropertyInitializerBody(ktDelegateExpression)
        irClass.addMember(irDelegate)

        // TODO add delegated members
    }

    private fun generateAdditionalMembersForDataClass(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(ktClassOrObject, context, irClass).generate()
    }

    private fun generateAdditionalMembersForEnumClass(irClass: IrClassImpl) {
        EnumClassMembersGenerator(context).generateSpecialMembers(irClass)
    }

    private fun generatePrimaryConstructor(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        val primaryConstructorDescriptor = irClass.descriptor.unsubstitutedPrimaryConstructor ?: return

        val irPrimaryConstructor = IrConstructorImpl(ktClassOrObject.startOffset, ktClassOrObject.endOffset, IrDeclarationOrigin.DEFINED,
                                                     primaryConstructorDescriptor)

        val bodyGenerator = BodyGenerator(primaryConstructorDescriptor, context)
        ktClassOrObject.getPrimaryConstructor()?.valueParameterList?.let { ktValueParameterList ->
            bodyGenerator.generateDefaultParameters(ktValueParameterList, irPrimaryConstructor)
        }
        irPrimaryConstructor.body = bodyGenerator.generatePrimaryConstructorBody(ktClassOrObject)

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
                val irMember = declarationGenerator.generateClassMemberDeclaration(ktDeclaration, irClass.descriptor)
                irClass.addMember(irMember)
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

    fun generateEnumEntry(ktEnumEntry: KtEnumEntry): IrEnumEntry {
        val enumEntryDescriptor = getOrFail(BindingContext.CLASS, ktEnumEntry)
        val irEnumEntry = IrEnumEntryImpl(ktEnumEntry.startOffset, ktEnumEntry.endOffset, IrDeclarationOrigin.DEFINED, enumEntryDescriptor)

        irEnumEntry.initializerExpression =
                BodyGenerator(enumEntryDescriptor.containingDeclaration, context)
                        .generateEnumEntryInitializer(ktEnumEntry, enumEntryDescriptor)

        if (ktEnumEntry.declarations.isNotEmpty()) {
            irEnumEntry.correspondingClass = generateClass(ktEnumEntry)
        }

        return irEnumEntry
    }
}
