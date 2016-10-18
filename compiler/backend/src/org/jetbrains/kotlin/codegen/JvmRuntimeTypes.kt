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

import org.jetbrains.kotlin.coroutines.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

import java.util.*

class JvmRuntimeTypes(module: ModuleDescriptor) {
    private val lambda: ClassDescriptor
    private val functionReference: ClassDescriptor
    private val propertyReferences: MutableList<ClassDescriptor>
    private val mutablePropertyReferences: MutableList<ClassDescriptor>
    private val localVariableReference: ClassDescriptor
    private val mutableLocalVariableReference: ClassDescriptor
    private val defaultContinuationSupertype: KotlinType

    init {
        val kotlinJvmInternal = MutablePackageFragmentDescriptor(module, FqName("kotlin.jvm.internal"))

        this.lambda = createClass(kotlinJvmInternal, "Lambda")
        this.functionReference = createClass(kotlinJvmInternal, "FunctionReference")
        this.localVariableReference = createClass(kotlinJvmInternal, "LocalVariableReference")
        this.mutableLocalVariableReference = createClass(kotlinJvmInternal, "MutableLocalVariableReference")
        this.propertyReferences = ArrayList<ClassDescriptor>(3)
        this.mutablePropertyReferences = ArrayList<ClassDescriptor>(3)

        for (i in 0..2) {
            propertyReferences.add(createClass(kotlinJvmInternal, "PropertyReference" + i))
            mutablePropertyReferences.add(createClass(kotlinJvmInternal, "MutablePropertyReference" + i))
        }

        defaultContinuationSupertype = createNullableAnyContinuation(module)
    }

    /**
     * @param module
     * *
     * @return Continuation<Any></Any>?> type
     */
    private fun createNullableAnyContinuation(module: ModuleDescriptor): KotlinType {
        val classDescriptor = module.resolveTopLevelClass(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME, NoLookupLocation.FROM_BACKEND) ?: error(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME + " was not found in built-ins")


        return TypeConstructorSubstitution
                .createByParametersMap(Collections.singletonMap(classDescriptor.declaredTypeParameters[0],
                                                                TypeProjectionImpl(module.builtIns.nullableAnyType)))
                .buildSubstitutor().substitute(classDescriptor.defaultType, Variance.INVARIANT)
    }

    private fun createClass(packageFragment: PackageFragmentDescriptor, name: String): ClassDescriptor {
        val descriptor = MutableClassDescriptor(
                packageFragment, ClassKind.CLASS, false, Name.identifier(name), SourceElement.NO_SOURCE
        )

        descriptor.modality = Modality.FINAL
        descriptor.visibility = Visibilities.PUBLIC
        descriptor.setTypeParameterDescriptors(emptyList<TypeParameterDescriptor>())
        descriptor.createTypeConstructor()

        return descriptor
    }

    fun getSupertypesForClosure(descriptor: FunctionDescriptor): Collection<KotlinType> {
        val receiverParameter = descriptor.extensionReceiverParameter


        val parameters = descriptor.valueParameters
        val functionType = createFunctionType(
                descriptor.builtIns,
                Annotations.EMPTY,
                receiverParameter?.type,
                ExpressionTypingUtils.getValueParametersTypes(parameters),
                null,
                descriptor.returnType!!
        )

        val coroutineControllerType = descriptor.controllerTypeIfCoroutine

        if (coroutineControllerType != null) {
            return Arrays.asList(
                    lambda.defaultType, functionType, /*coroutineType,*/ defaultContinuationSupertype)
        }

        return Arrays.asList<KotlinType>(lambda.defaultType, functionType)
    }

    fun getSupertypesForFunctionReference(descriptor: FunctionDescriptor, isBound: Boolean): Collection<KotlinType> {
        val extensionReceiver = descriptor.extensionReceiverParameter
        val dispatchReceiver = descriptor.dispatchReceiverParameter

        val receiverType = if (extensionReceiver != null) extensionReceiver.type else dispatchReceiver?.type


        val parameters = descriptor.valueParameters
        val functionType = createFunctionType(
                descriptor.builtIns,
                Annotations.EMPTY,
                if (isBound) null else receiverType,
                ExpressionTypingUtils.getValueParametersTypes(parameters),
                null,
                descriptor.returnType!!
        )

        return Arrays.asList<KotlinType>(functionReference.defaultType, functionType)
    }

    fun getSupertypeForPropertyReference(
            descriptor: VariableDescriptorWithAccessors, isMutable: Boolean, isBound: Boolean
    ): KotlinType {
        if (descriptor is LocalVariableDescriptor) {
            return (if (isMutable) mutableLocalVariableReference else localVariableReference).defaultType
        }

        val arity = (if (descriptor.extensionReceiverParameter != null) 1 else 0) + (if (descriptor.dispatchReceiverParameter != null) 1 else 0) - if (isBound) 1 else 0
        return (if (isMutable) mutablePropertyReferences else propertyReferences)[arity].defaultType
    }
}
