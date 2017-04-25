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
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.calls.inference.isCaptured
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
        val isCaptured = type.isCaptured()
        val typeToSerialize = if (isCaptured) {
            packCapturedType(type as CapturedType)
        } else {
            type
        }
        val index = typeSerializer(typeToSerialize)
        val proto = KonanIr.KotlinType.newBuilder()
            .setIndex(index)
            .setDebugText(type.toString())
            .setIsCaptured(isCaptured)
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

        val typeParameters = descriptor.propertyIfAccessor.typeParameters
        typeParameters.forEach {
            proto.addTypeParameter(serializeDescriptor(it))
            // We explicitly serialize type parameters
            // as types here so that they are interned in the 
            // natural order of declaration. Otherwise 
            // they appear in the order of appearence in the 
            // body of the function, and get wrong indices.
            typeSerializer(it.defaultType)
        }

        descriptor.valueParameters.forEach {
            proto.addValueParameter(serializeDescriptor(it))
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
        } else null

        val index = descriptorTable.indexByValue(descriptor)
        // For getters and setters we use 
        // the *property* original index.
        val originalIndex = descriptorTable.indexByValue(descriptor.propertyIfAccessor.original)

        context.log{"index = $index"}
        context.log{"originalIndex = $originalIndex"}
        context.log{""}

        val proto =  KonanIr.KotlinDescriptor.newBuilder()
            .setName(descriptor.name.asString())
            .setKind(kotlinDescriptorKind(descriptor))
            .setIndex(index)
            .setOriginalIndex(originalIndex)

        if (parentFqNameIndex != null)
            proto.setClassOrPackage(parentFqNameIndex)

        when (descriptor) {
            is FunctionDescriptor ->
                functionDescriptorSpecifics(descriptor, proto)
            is VariableDescriptor ->
                variableDescriptorSpecifics(descriptor, proto)
        }

        return proto.build()
    }

}

