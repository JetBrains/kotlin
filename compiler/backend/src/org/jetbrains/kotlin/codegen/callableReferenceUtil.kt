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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.isGetterOfUnderlyingPropertyOfInlineClass
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.shouldHideConstructorDueToInlineClassTypeValueParameters
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

fun capturedBoundReferenceReceiver(
    ownerType: Type,
    expectedReceiverType: Type,
    expectedReceiverKotlinType: KotlinType?,
    isInliningStrategy: Boolean
): StackValue =
    StackValue.operation(expectedReceiverType) { iv ->
        iv.load(0, ownerType)
        iv.getfield(
            ownerType.internalName,
            //HACK for inliner - it should recognize field as captured receiver
            if (isInliningStrategy) AsmUtil.CAPTURED_RECEIVER_FIELD else AsmUtil.BOUND_REFERENCE_RECEIVER,
            AsmTypes.OBJECT_TYPE.descriptor
        )
        val nullableAny = expectedReceiverKotlinType?.run { builtIns.nullableAnyType }
        StackValue.coerce(AsmTypes.OBJECT_TYPE, nullableAny, expectedReceiverType, expectedReceiverKotlinType, iv)
    }

fun ClassDescriptor.isSyntheticClassForCallableReference(): Boolean =
    this is SyntheticClassDescriptorForLambda &&
            (this.source as? KotlinSourceElement)?.psi is KtCallableReferenceExpression

fun CalculatedClosure.isForCallableReference(): Boolean =
    closureClass.isSyntheticClassForCallableReference()

fun CalculatedClosure.isForBoundCallableReference(): Boolean =
    isForCallableReference() && capturedReceiverFromOuterContext != null

fun InstructionAdapter.loadBoundReferenceReceiverParameter(index: Int, type: Type, kotlinType: KotlinType?) {
    load(index, type)
    val nullableAny = kotlinType?.run { builtIns.nullableAnyType }
    StackValue.coerce(type, kotlinType, AsmTypes.OBJECT_TYPE, nullableAny, this)
}

fun CalculatedClosure.isBoundReferenceReceiverField(fieldInfo: FieldInfo): Boolean =
    isForBoundCallableReference() &&
            fieldInfo.fieldName == AsmUtil.CAPTURED_RECEIVER_FIELD

fun InstructionAdapter.generateClosureFieldsInitializationFromParameters(
    closure: CalculatedClosure,
    args: List<FieldInfo>
): Pair<Int, FieldInfo>? {
    var k = 1
    var boundReferenceReceiverParameterIndex = -1
    var boundReferenceReceiverFieldInfo: FieldInfo? = null
    for (fieldInfo in args) {
        if (closure.isBoundReferenceReceiverField(fieldInfo)) {
            boundReferenceReceiverParameterIndex = k
            boundReferenceReceiverFieldInfo = fieldInfo
            k += fieldInfo.fieldType.size
            continue
        }
        k = DescriptorAsmUtil.genAssignInstanceFieldFromParam(fieldInfo, k, this)
    }

    return boundReferenceReceiverFieldInfo?.let { Pair(boundReferenceReceiverParameterIndex, it) }
}

fun computeExpectedNumberOfReceivers(referencedFunction: FunctionDescriptor, isBound: Boolean): Int {
    val receivers = referencedFunction.contextReceiverParameters.size +
            (if (referencedFunction.dispatchReceiverParameter != null) 1 else 0) +
            (if (referencedFunction.extensionReceiverParameter != null) 1 else 0) -
            (if (isBound) 1 else 0)

    if (receivers < 0 && referencedFunction is ConstructorDescriptor &&
        DescriptorUtils.isObject(referencedFunction.containingDeclaration.containingDeclaration)
    ) {
        //reference to object nested class
        //TODO: seems problem should be fixed on frontend side (note that object instance are captured by generated class)
        return 0
    }

    return receivers
}

// Returns false if null was generated.
internal fun generateCallableReferenceDeclarationContainerClass(
    iv: InstructionAdapter,
    descriptor: CallableDescriptor,
    state: GenerationState
): Boolean {
    val typeMapper = state.typeMapper
    val container = descriptor.containingDeclaration
    when {
        container is ClassDescriptor -> {
            // TODO: would it work for arrays?
            val containerKotlinType = container.defaultType
            val containerType = typeMapper.mapClass(container)
            DescriptorAsmUtil.putJavaLangClassInstance(iv, containerType, containerKotlinType, typeMapper)
        }
        container is PackageFragmentDescriptor -> {
            iv.aconst(typeMapper.mapOwner(descriptor))
        }
        descriptor is VariableDescriptorWithAccessors -> {
            iv.aconst(state.bindingContext[CodegenBinding.DELEGATED_PROPERTY_METADATA_OWNER, descriptor])
        }
        else -> {
            iv.aconst(null)
            return false
        }
    }
    return true
}

internal fun generateCallableReferenceDeclarationContainer(
    iv: InstructionAdapter,
    descriptor: CallableDescriptor,
    state: GenerationState
) {
    if (!generateCallableReferenceDeclarationContainerClass(iv, descriptor, state)) return
    if (isTopLevelCallableReference(descriptor)) {
        // Note that this name is not used in reflection. There should be the name of the referenced declaration's module instead,
        // but there's no nice API to obtain that name here yet
        // TODO: write the referenced declaration's module name and use it in reflection
        iv.aconst(state.moduleName)
        iv.invokestatic(
            AsmTypes.REFLECTION, "getOrCreateKotlinPackage",
            Type.getMethodDescriptor(
                AsmTypes.K_DECLARATION_CONTAINER_TYPE, AsmTypes.getType(Class::class.java), AsmTypes.getType(String::class.java)
            ), false
        )
    } else {
        AsmUtil.wrapJavaClassIntoKClass(iv)
    }
}

private fun isTopLevelCallableReference(descriptor: CallableDescriptor): Boolean =
    if (descriptor is LocalVariableDescriptor)
        DescriptorUtils.getParentOfType(descriptor, ClassDescriptor::class.java) == null
    else descriptor.containingDeclaration is PackageFragmentDescriptor

internal fun getCallableReferenceTopLevelFlag(descriptor: CallableDescriptor): Int =
    if (isTopLevelCallableReference(descriptor)) 1 else 0

internal fun generateFunctionReferenceSignature(iv: InstructionAdapter, callable: CallableDescriptor, state: GenerationState) {
    iv.aconst(getSignatureString(callable, state))
}

internal fun generatePropertyReferenceSignature(iv: InstructionAdapter, callable: CallableDescriptor, state: GenerationState) {
    iv.aconst(getSignatureString(callable, state, isPropertySignature = true))
}

private fun getSignatureString(callable: CallableDescriptor, state: GenerationState, isPropertySignature: Boolean = false): String {
    if (callable is LocalVariableDescriptor) {
        val asmType = state.bindingContext.get(CodegenBinding.DELEGATED_PROPERTY_METADATA_OWNER, callable)
            ?: throw AssertionError("No delegated property metadata owner for $callable")
        val localDelegatedProperties = CodegenBinding.getLocalDelegatedProperties(state.bindingContext, asmType)
        val index = localDelegatedProperties?.indexOf(callable) ?: -1
        if (index < 0) {
            throw AssertionError("Local delegated property is not found in $asmType: $callable")
        }
        return "<v#$index>"
    }

    val accessor = when (callable) {
        is ClassConstructorDescriptor ->
            if (shouldHideConstructorDueToInlineClassTypeValueParameters(callable))
                AccessorForConstructorDescriptor(callable, callable.containingDeclaration, null, AccessorKind.NORMAL)
            else
                callable
        is FunctionDescriptor -> callable
        is VariableDescriptorWithAccessors ->
            callable.getter ?: DescriptorFactory.createDefaultGetter(callable as PropertyDescriptor, Annotations.EMPTY).apply {
                initialize(callable.type)
            }
        else -> error("Unsupported callable reference: $callable")
    }
    val declaration = DescriptorUtils.unwrapFakeOverride(accessor).original
    val method = when {
        callable.containingDeclaration.isInlineClass() && !declaration.isGetterOfUnderlyingPropertyOfInlineClass() ->
            state.typeMapper.mapSignatureForInlineErasedClassSkipGeneric(declaration).asmMethod
        isPropertySignature ->
            state.typeMapper.mapPropertyReferenceSignature(declaration)
        else ->
            state.typeMapper.mapAsmMethod(declaration)
    }
    return method.name + method.descriptor
}
