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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import java.lang.AssertionError
import java.util.*

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

    private object StableDelegatesComparator : Comparator<CallableMemberDescriptor> {
        override fun compare(member1: CallableMemberDescriptor?, member2: CallableMemberDescriptor?): Int {
            if (member1 == member2) return 0
            if (member1 == null) return -1
            if (member2 == null) return 1

            val image1 = DESCRIPTOR_RENDERER.render(member1)
            val image2 = DESCRIPTOR_RENDERER.render(member2)
            return image1.compareTo(image2)
        }

        private val DESCRIPTOR_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            includePropertyConstant = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            verbose = true
            modifiers = DescriptorRendererModifier.ALL
        }
    }

    private fun generateMembersDeclaredInSupertypeList(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getSuperTypeList()?.let { ktSuperTypeList ->
            val delegatedMembers = irClass.descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { it.kind == CallableMemberDescriptor.Kind.DELEGATION }
                    .sortedWith(StableDelegatesComparator)
            if (delegatedMembers.isEmpty()) return

            for (ktEntry in ktSuperTypeList.entries) {
                if (ktEntry is KtDelegatedSuperTypeEntry) {
                    generateDelegatedImplementationMembers(irClass, ktEntry, delegatedMembers)
                }
            }
        }
    }

    private fun generateDelegatedImplementationMembers(irClass: IrClassImpl, ktEntry: KtDelegatedSuperTypeEntry,
                                                       delegatedMembers: List<CallableMemberDescriptor>) {
        val ktDelegateExpression = ktEntry.delegateExpression!!
        val delegateType = getInferredTypeWithImplicitCastsOrFail(ktDelegateExpression)
        val superType = getOrFail(BindingContext.TYPE, ktEntry.typeReference!!)
        val superTypeConstructorDescriptor = superType.constructor.declarationDescriptor
        val superClass = superTypeConstructorDescriptor as? ClassDescriptor ?:
                         throw AssertionError("Unexpected supertype constructor for delegation: $superTypeConstructorDescriptor")
        val delegateDescriptor = IrImplementingDelegateDescriptorImpl(irClass.descriptor, delegateType, superType)
        val irDelegate = IrFieldImpl(ktDelegateExpression.startOffset, ktDelegateExpression.endOffset, IrDeclarationOrigin.DELEGATE,
                                     delegateDescriptor)
        val bodyGenerator = BodyGenerator(irClass.descriptor, context)
        irDelegate.initializer = bodyGenerator.generatePropertyInitializerBody(ktDelegateExpression)
        irClass.addMember(irDelegate)

        for (delegatedMember in delegatedMembers) {
            val overriddenMember = delegatedMember.overriddenDescriptors.find { it.containingDeclaration.original == superClass.original }
            if (overriddenMember != null) {
                generateDelegatedMember(irClass, irDelegate, delegatedMember, overriddenMember)
            }
        }
    }

    private fun generateDelegatedMember(irClass: IrClassImpl, irDelegate: IrFieldImpl,
                                        delegatedMember: CallableMemberDescriptor, overriddenMember: CallableMemberDescriptor) {
        when (delegatedMember) {
            is FunctionDescriptor ->
                generateDelegatedFunction(irClass, irDelegate, delegatedMember, overriddenMember as FunctionDescriptor)
            is PropertyDescriptor ->
                generateDelegatedProperty(irClass, irDelegate, delegatedMember, overriddenMember as PropertyDescriptor)
        }

    }

    private fun generateDelegatedProperty(irClass: IrClassImpl, irDelegate: IrFieldImpl,
                                          delegated: PropertyDescriptor, overridden: PropertyDescriptor) {
        val irProperty = IrPropertyImpl(irDelegate.startOffset, irDelegate.endOffset, IrDeclarationOrigin.DELEGATED_MEMBER, false, delegated)

        val irGetter = IrFunctionImpl(irDelegate.startOffset, irDelegate.endOffset, IrDeclarationOrigin.DELEGATED_MEMBER, delegated.getter!!)
        irGetter.body = generateDelegateFunctionBody(irDelegate, delegated.getter!!, overridden.getter!!)
        irProperty.getter = irGetter

        if (delegated.isVar) {
            val irSetter = IrFunctionImpl(irDelegate.startOffset, irDelegate.endOffset, IrDeclarationOrigin.DELEGATED_MEMBER, delegated.setter!!)
            irSetter.body = generateDelegateFunctionBody(irDelegate, delegated.setter!!, overridden.setter!!)
            irProperty.setter = irSetter
        }

        irClass.addMember(irProperty)
    }

    private fun generateDelegatedFunction(irClass: IrClassImpl, irDelegate: IrFieldImpl, delegated: FunctionDescriptor, overridden: FunctionDescriptor) {
        val irFunction = IrFunctionImpl(irDelegate.startOffset, irDelegate.endOffset, IrDeclarationOrigin.DELEGATED_MEMBER, delegated)
        irFunction.body = generateDelegateFunctionBody(irDelegate, delegated, overridden)
        irClass.addMember(irFunction)
    }

    private fun generateDelegateFunctionBody(irDelegate: IrFieldImpl, delegated: FunctionDescriptor, overridden: FunctionDescriptor): IrBlockBodyImpl {
        val irBlockBody = IrBlockBodyImpl(irDelegate.startOffset, irDelegate.endOffset)
        val returnType = overridden.returnType!!
        val irCall = IrCallImpl(irDelegate.startOffset, irDelegate.endOffset, returnType, overridden)
        irCall.dispatchReceiver = IrGetVariableImpl(irDelegate.startOffset, irDelegate.endOffset, irDelegate.descriptor)
        irCall.extensionReceiver = delegated.extensionReceiverParameter?.let { extensionReceiver ->
            IrGetExtensionReceiverImpl(irDelegate.startOffset, irDelegate.endOffset, extensionReceiver)
        }
        irCall.mapValueParameters { overriddenValueParameter ->
            val delegatedValueParameter = delegated.valueParameters[overriddenValueParameter.index]
            IrGetVariableImpl(irDelegate.startOffset, irDelegate.endOffset, delegatedValueParameter)
        }
        if (KotlinBuiltIns.isUnit(returnType) || KotlinBuiltIns.isNothing(returnType)) {
            irBlockBody.addStatement(irCall)
        }
        else {
            val irReturn = IrReturnImpl(irDelegate.startOffset, irDelegate.endOffset, context.builtIns.nothingType, delegated, irCall)
            irBlockBody.addStatement(irReturn)
        }
        return irBlockBody
    }

    private fun generateAdditionalMembersForDataClass(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(ktClassOrObject, context, irClass).generate()
    }

    private fun generateAdditionalMembersForEnumClass(irClass: IrClassImpl) {
        EnumClassMembersGenerator(context).generateSpecialMembers(irClass)
    }

    private fun generatePrimaryConstructor(irClass: IrClassImpl, ktClassOrObject: KtClassOrObject) {
        val classDescriptor = irClass.descriptor
        if (DescriptorUtils.isAnnotationClass(classDescriptor)) return

        val primaryConstructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor ?: return

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
                    val irProperty = PropertyGenerator(declarationGenerator).generatePropertyForPrimaryConstructorParameter(ktParameter)
                    irClass.addMember(irProperty)
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
