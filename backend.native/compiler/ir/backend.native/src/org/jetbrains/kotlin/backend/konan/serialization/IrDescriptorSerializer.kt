/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.KonanIr
import org.jetbrains.kotlin.types.KotlinType

val DeclarationDescriptor.classOrPackage: DeclarationDescriptor?
    get() {
        return if (this.containingDeclaration!! is ClassOrPackageFragmentDescriptor) 
            containingDeclaration 
        else null
    }

internal class IrDescriptorSerializer(
    val context: Context,
    val descriptorTable: DescriptorTable,
    val stringTable: KonanStringTable,
    var typeSerializer: ((KotlinType)->Int),
    var rootFunction: FunctionDescriptor) {

    fun serializeKotlinType(type: KotlinType): KonanIr.KotlinType {
        val index = this.typeSerializer(type)
        val proto =  KonanIr.KotlinType.newBuilder()
            .setIndex(index)
            .setDebugText(type.toString())
            .build()
        return proto
    }

    fun kotlinDescriptorKind(descriptor: DeclarationDescriptor) =
        when (descriptor) {
            is ConstructorDescriptor 
                -> KonanIr.KotlinDescriptor.Kind.CONSTRUCTOR
            is PropertyAccessorDescriptor
                -> KonanIr.KotlinDescriptor.Kind.ACCESSOR
            is FunctionDescriptor 
                -> KonanIr.KotlinDescriptor.Kind.FUNCTION
            is ClassDescriptor 
                -> KonanIr.KotlinDescriptor.Kind.CLASS
            is ValueParameterDescriptor 
                -> KonanIr.KotlinDescriptor.Kind.VALUE_PARAMETER
            is LocalVariableDescriptor, 
            is IrTemporaryVariableDescriptor 
                -> KonanIr.KotlinDescriptor.Kind.VARIABLE
            is TypeParameterDescriptor
                -> KonanIr.KotlinDescriptor.Kind.TYPE_PARAMETER
            is ReceiverParameterDescriptor
                -> KonanIr.KotlinDescriptor.Kind.RECEIVER
            else -> TODO("Unexpected local descriptor.")
        }

    fun functionDescriptorSpecifics(descriptor: FunctionDescriptor, proto: KonanIr.KotlinDescriptor.Builder) {

        descriptor.valueParameters.forEach {
            proto.addValueParameter(serializeDescriptor(it))
        }

        val typeParameters = if (descriptor is PropertyAccessorDescriptor) 
                descriptor.correspondingProperty.typeParameters
        else descriptor.typeParameters

        typeParameters.forEach {
            proto.addTypeParameter(serializeDescriptor(it))
        }

        // Allocate two indicies for the receivers.
        // They are not deserialized from protobuf, 
        // just recreated together with their function.

        val dispatchReceiver = descriptor.dispatchReceiverParameter
        if (dispatchReceiver != null) 
            proto.setDispatchReceiverIndex(
                descriptorTable.indexByValue(dispatchReceiver))

        val extensionReceiver = descriptor.extensionReceiverParameter
        if (extensionReceiver != null) {
            proto.setExtensionReceiverIndex(
                descriptorTable.indexByValue(extensionReceiver))
            proto.setExtensionReceiverType(
                serializeKotlinType(extensionReceiver.type))
        }
                
        proto.setType(serializeKotlinType(
            descriptor.returnType!!)) 

    }

    fun variableDescriptorSpecifics(descriptor: VariableDescriptor, proto: KonanIr.KotlinDescriptor.Builder) {
        proto.setType(serializeKotlinType(descriptor.type))
    }

    fun serializeDescriptor(descriptor: DeclarationDescriptor): KonanIr.KotlinDescriptor {

        if (descriptor is CallableMemberDescriptor &&  
            descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            // TODO: It seems rather braindead. 
            // Do we need to do anything more than that?
            return serializeDescriptor(DescriptorUtils.unwrapFakeOverride(descriptor))
        }

        val classOrPackage = descriptor.classOrPackage
        val parentFqNameIndex = if (classOrPackage is ClassOrPackageFragmentDescriptor) {
            stringTable.getClassOrPackageFqNameIndex(classOrPackage)
        } else { -1 }

        val index = descriptorTable.indexByValue(descriptor)
        // For getters and setters we use 
        // the *property* original index.
        val originalIndex = if (descriptor is PropertyAccessorDescriptor) {
            descriptorTable.indexByValue(descriptor.correspondingProperty.original)
        } else {
            descriptorTable.indexByValue(descriptor.original)
        }

        context.log("index = $index")
        context.log("originalIndex = $originalIndex")
        context.log("")

        val proto =  KonanIr.KotlinDescriptor.newBuilder()
            .setName(descriptor.name.asString())
            .setKind(kotlinDescriptorKind(descriptor))
            .setIndex(index)
            .setOriginalIndex(originalIndex)
            .setClassOrPackage(parentFqNameIndex)

        when (descriptor) {
            is FunctionDescriptor ->
                functionDescriptorSpecifics(descriptor, proto)
            is VariableDescriptor ->
                variableDescriptorSpecifics(descriptor, proto)
        }

        return proto.build()
    }

}

