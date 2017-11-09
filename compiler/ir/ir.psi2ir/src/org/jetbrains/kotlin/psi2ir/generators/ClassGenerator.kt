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
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.mapValueParameters
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.ir.util.StableDescriptorsComparator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.lang.AssertionError

class ClassGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generateClass(ktClassOrObject: KtClassOrObject): IrClass {
        val descriptor = getOrFail(BindingContext.CLASS, ktClassOrObject)

        return context.symbolTable.declareClass(
                ktClassOrObject.startOffset, ktClassOrObject.endOffset,
                IrDeclarationOrigin.DEFINED,
                descriptor
        ).buildWithScope { irClass ->
            irClass.thisReceiver = context.symbolTable.declareValueParameter(
                    ktClassOrObject.startOffset, ktClassOrObject.endOffset,
                    IrDeclarationOrigin.INSTANCE_RECEIVER,
                    irClass.descriptor.thisAsReceiverParameter
            )

            declarationGenerator.generateTypeParameterDeclarations(irClass, descriptor.declaredTypeParameters)

            val irPrimaryConstructor = generatePrimaryConstructor(irClass, ktClassOrObject)
            if (irPrimaryConstructor != null) {
                generateDeclarationsForPrimaryConstructorParameters(irClass, irPrimaryConstructor, ktClassOrObject)
            }

            generateMembersDeclaredInSupertypeList(irClass, ktClassOrObject)

            generateMembersDeclaredInClassBody(irClass, ktClassOrObject)

            generateFakeOverrideMemberDeclarations(irClass, ktClassOrObject)

            if (descriptor.isData) {
                generateAdditionalMembersForDataClass(irClass, ktClassOrObject)
            }

            if (descriptor.kind == ClassKind.ENUM_CLASS) {
                generateAdditionalMembersForEnumClass(irClass)
            }
        }
    }

    private fun generateFakeOverrideMemberDeclarations(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        irClass.descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                .mapNotNull {
                    it.safeAs<CallableMemberDescriptor>().takeIf {
                        it?.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                    }
                }
                .sortedWith(StableDescriptorsComparator)
                .forEach { fakeOverride ->
                    irClass.addMember(declarationGenerator.generateFakeOverrideDeclaration(fakeOverride, ktClassOrObject))
                }
    }

    private fun generateMembersDeclaredInSupertypeList(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getSuperTypeList()?.let { ktSuperTypeList ->
            val delegatedMembers = irClass.descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { it.kind == CallableMemberDescriptor.Kind.DELEGATION }
                    .sortedWith(StableDescriptorsComparator)
            if (delegatedMembers.isEmpty()) return

            for (ktEntry in ktSuperTypeList.entries) {
                if (ktEntry is KtDelegatedSuperTypeEntry) {
                    generateDelegatedImplementationMembers(irClass, ktEntry, delegatedMembers)
                }
            }
        }
    }

    private fun generateDelegatedImplementationMembers(
            irClass: IrClass,
            ktEntry: KtDelegatedSuperTypeEntry,
            delegatedMembers: List<CallableMemberDescriptor>
    ) {
        val ktDelegateExpression = ktEntry.delegateExpression!!
        val delegateType = getInferredTypeWithImplicitCastsOrFail(ktDelegateExpression)
        val superType = getOrFail(BindingContext.TYPE, ktEntry.typeReference!!)
        val superTypeConstructorDescriptor = superType.constructor.declarationDescriptor
        val superClass = superTypeConstructorDescriptor as? ClassDescriptor ?:
                         throw AssertionError("Unexpected supertype constructor for delegation: $superTypeConstructorDescriptor")
        val delegateDescriptor = IrImplementingDelegateDescriptorImpl(irClass.descriptor, delegateType, superType)
        val irDelegateField = context.symbolTable.declareField(
                ktDelegateExpression.startOffset, ktDelegateExpression.endOffset,
                IrDeclarationOrigin.DELEGATE,
                delegateDescriptor,
                createBodyGenerator(irClass.symbol).generateExpressionBody(ktDelegateExpression)
        )
        irClass.addMember(irDelegateField)

        for (delegatedMember in delegatedMembers) {
            val overriddenMember = delegatedMember.overriddenDescriptors.find { it.containingDeclaration.original == superClass.original }
            if (overriddenMember != null) {
                generateDelegatedMember(irClass, irDelegateField, delegatedMember, overriddenMember)
            }
        }
    }

    private fun generateDelegatedMember(irClass: IrClass, irDelegate: IrField,
                                        delegatedMember: CallableMemberDescriptor, overriddenMember: CallableMemberDescriptor) {
        when (delegatedMember) {
            is FunctionDescriptor ->
                generateDelegatedFunction(irClass, irDelegate, delegatedMember, overriddenMember as FunctionDescriptor)
            is PropertyDescriptor ->
                generateDelegatedProperty(irClass, irDelegate, delegatedMember, overriddenMember as PropertyDescriptor)
        }

    }

    private fun generateDelegatedProperty(irClass: IrClass, irDelegate: IrField, delegated: PropertyDescriptor, overridden: PropertyDescriptor) {
        irClass.addMember(generateDelegatedProperty(irDelegate, delegated, overridden))
    }

    private fun generateDelegatedProperty(irDelegate: IrField, delegated: PropertyDescriptor, overridden: PropertyDescriptor): IrPropertyImpl {
        val startOffset = irDelegate.startOffset
        val endOffset = irDelegate.endOffset

        val irProperty = IrPropertyImpl(startOffset, endOffset, IrDeclarationOrigin.DELEGATED_MEMBER, false, delegated)

        irProperty.getter = generateDelegatedFunction(irDelegate, delegated.getter!!, overridden.getter!!)

        if (delegated.isVar) {
            irProperty.setter = generateDelegatedFunction(irDelegate, delegated.setter!!, overridden.setter!!)
        }
        return irProperty
    }

    private fun generateDelegatedFunction(irClass: IrClass, irDelegate: IrField, delegated: FunctionDescriptor, overridden: FunctionDescriptor) {
        irClass.addMember(generateDelegatedFunction(irDelegate, delegated, overridden))
    }

    private fun generateDelegatedFunction(irDelegate: IrField, delegated: FunctionDescriptor, overridden: FunctionDescriptor): IrFunction =
            context.symbolTable.declareSimpleFunction(
                    irDelegate.startOffset, irDelegate.endOffset,
                    IrDeclarationOrigin.DELEGATED_MEMBER,
                    delegated
            ).buildWithScope { irFunction ->
                FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)
                irFunction.body = generateDelegateFunctionBody(irDelegate, delegated, overridden, irFunction)
            }

    private fun generateDelegateFunctionBody(irDelegate: IrField, delegated: FunctionDescriptor, overridden: FunctionDescriptor,
                                             irDelegatedFunction: IrSimpleFunction): IrBlockBodyImpl {
        val startOffset = irDelegate.startOffset
        val endOffset = irDelegate.endOffset
        val irBlockBody = IrBlockBodyImpl(startOffset, endOffset)
        val returnType = overridden.returnType!!
        val irCall = IrCallImpl(startOffset, endOffset, returnType, context.symbolTable.referenceFunction(overridden.original), overridden, null)
        irCall.dispatchReceiver =
                IrGetFieldImpl(
                        startOffset, endOffset, irDelegate.symbol,
                        IrGetValueImpl(startOffset, endOffset, irDelegatedFunction.dispatchReceiverParameter!!.symbol)
                )
        irCall.extensionReceiver =
                irDelegatedFunction.extensionReceiverParameter?.let { extensionReceiver ->
                    IrGetValueImpl(startOffset, endOffset, extensionReceiver.symbol)
                }
        irCall.mapValueParameters { overriddenValueParameter ->
            val delegatedValueParameter = delegated.valueParameters[overriddenValueParameter.index]
            val irDelegatedValueParameter = irDelegatedFunction.getIrValueParameter(delegatedValueParameter)
            IrGetValueImpl(startOffset, endOffset, irDelegatedValueParameter.symbol)
        }
        if (KotlinBuiltIns.isUnit(returnType) || KotlinBuiltIns.isNothing(returnType)) {
            irBlockBody.statements.add(irCall)
        }
        else {
            val irReturn = IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType, irDelegatedFunction.symbol, irCall)
            irBlockBody.statements.add(irReturn)
        }
        return irBlockBody
    }

    private fun generateAdditionalMembersForDataClass(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        DataClassMembersGenerator(declarationGenerator).generate(ktClassOrObject, irClass)
    }

    private fun generateAdditionalMembersForEnumClass(irClass: IrClass) {
        EnumClassMembersGenerator(declarationGenerator).generateSpecialMembers(irClass)
    }

    private fun generatePrimaryConstructor(irClass: IrClass, ktClassOrObject: KtClassOrObject): IrConstructor? {
        val classDescriptor = irClass.descriptor
        val primaryConstructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor ?: return null

        val irPrimaryConstructor = FunctionGenerator(declarationGenerator).generatePrimaryConstructor(primaryConstructorDescriptor, ktClassOrObject)

        if (!DescriptorUtils.isAnnotationClass(classDescriptor)) {
            irClass.addMember(irPrimaryConstructor)
        }

        return irPrimaryConstructor
    }

    private fun generateDeclarationsForPrimaryConstructorParameters(
            irClass: IrClass,
            irPrimaryConstructor: IrConstructor,
            ktClassOrObject: KtClassOrObject
    ) {
        ktClassOrObject.primaryConstructor?.let { ktPrimaryConstructor ->
            irPrimaryConstructor.valueParameters.forEach {
                context.symbolTable.introduceValueParameter(it)
            }

            ktPrimaryConstructor.valueParameters.forEachIndexed { i, ktParameter ->
                val irValueParameter = irPrimaryConstructor.valueParameters[i]
                if (ktParameter.hasValOrVar()) {
                    val irProperty = PropertyGenerator(declarationGenerator)
                            .generatePropertyForPrimaryConstructorParameter(ktParameter, irValueParameter)
                    irClass.addMember(irProperty)
                }
            }
        }
    }

    private fun generateMembersDeclaredInClassBody(irClass: IrClass, ktClassOrObject: KtClassOrObject) {
        ktClassOrObject.getBody()?.let { ktClassBody ->
            ktClassBody.declarations.mapTo(irClass.declarations) { ktDeclaration ->
                declarationGenerator.generateClassMemberDeclaration(ktDeclaration, irClass.descriptor)
            }
        }
    }

    fun generateEnumEntry(ktEnumEntry: KtEnumEntry): IrEnumEntry {
        val enumEntryDescriptor = getOrFail(BindingContext.CLASS, ktEnumEntry)
        return context.symbolTable.declareEnumEntry(
                ktEnumEntry.startOffset,
                ktEnumEntry.endOffset,
                IrDeclarationOrigin.DEFINED,
                enumEntryDescriptor
        ).buildWithScope { irEnumEntry ->
            irEnumEntry.initializerExpression =
                    createBodyGenerator(irEnumEntry.symbol)
                            .generateEnumEntryInitializer(ktEnumEntry, enumEntryDescriptor)

            if (ktEnumEntry.declarations.isNotEmpty()) {
                irEnumEntry.correspondingClass = generateClass(ktEnumEntry)
            }
        }

    }
}
