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
import org.jetbrains.kotlin.backend.konan.KonanIrDeserializationException
import org.jetbrains.kotlin.backend.konan.descriptors.contributedMethods
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.createSynthesizedInvokes
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.KonanIr
import org.jetbrains.kotlin.serialization.KonanIr.KotlinDescriptor
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.types.*

internal fun DeclarationDescriptor.findPackage(): PackageFragmentDescriptor {
    return if (this is PackageFragmentDescriptor) this 
        else this.containingDeclaration!!.findPackage()
}

internal fun DeserializedMemberDescriptor.nameTable(): ProtoBuf.QualifiedNameTable {
    val pkg = this.findPackage()
    assert(pkg is KonanPackageFragment)
    return (pkg as KonanPackageFragment).proto.getNameTable()
}


internal fun DeserializedMemberDescriptor.nameResolver(): NameResolver {
    val pkg = this.findPackage() as KonanPackageFragment
    return NameResolverImpl(pkg.proto.getStringTable(), pkg.proto.getNameTable())
}

// This is the class that knowns how to deserialize
// Kotlin descriptors and types for IR.

internal class IrDescriptorDeserializer(val context: Context, 
    val rootDescriptor: DeserializedCallableMemberDescriptor,
    val localDeserializer: LocalDeclarationDeserializer) {

    val loopIndex = mutableMapOf<Int, IrLoop>()
    val nameResolver = rootDescriptor.nameResolver() as NameResolverImpl
    val nameTable = rootDescriptor.nameTable()
    val descriptorIndex = IrDeserializationDescriptorIndex(context.irBuiltIns).map

    fun deserializeKotlinType(proto: KonanIr.KotlinType): KotlinType {
        val index = proto.getIndex()
        val text = proto.getDebugText()
        val typeProto = localDeserializer.typeTable[index]
        val type = localDeserializer.deserializeInlineType(typeProto)
        if (type.isError) throw KonanIrDeserializationException("Could not deserialize KotlinType: $text $type")

        val realType = if (proto.isCaptured) {
            unpackCapturedType(type)
        } else
            type

        context.log{"### deserialized Kotlin Type index=$index, text=$text:\t$realType"}
        return realType
    }
    fun deserializeLocalDeclaration(irProto: KonanIr.KotlinDescriptor): DeclarationDescriptor {
        val localDeclarationProto = irProto.irLocalDeclaration.descriptor
        when {
            localDeclarationProto.hasFunction() -> {
                val index = irProto.index
                val descriptor = localDeserializer.deserializeFunction(irProto)
                descriptorIndex.put(index, descriptor)
                return descriptor
            }

            localDeclarationProto.hasProperty() -> {
                val index = irProto.index
                val descriptor = localDeserializer.deserializeProperty(irProto)
                descriptorIndex.put(index, descriptor)
                return descriptor
            }
            // TODO
            //  localDclarationProto.hasClazz() -> 
            else -> TODO("Unexpected descriptor kind")
        }
    }

    // Either (a substitution of) a public descriptor
    // or an already seen local declaration.
    fun deserializeKnownDescriptor(proto: KonanIr.KotlinDescriptor): DeclarationDescriptor {

        val index = proto.index
        val kind = proto.kind

        val descriptor = when (kind) {
            KonanIr.KotlinDescriptor.Kind.VALUE_PARAMETER,
            KonanIr.KotlinDescriptor.Kind.TYPE_PARAMETER,
            KonanIr.KotlinDescriptor.Kind.RECEIVER,
            KonanIr.KotlinDescriptor.Kind.VARIABLE ->
                descriptorIndex[index]!!

            KonanIr.KotlinDescriptor.Kind.CLASS,
            KonanIr.KotlinDescriptor.Kind.CONSTRUCTOR,
            KonanIr.KotlinDescriptor.Kind.FUNCTION,
            KonanIr.KotlinDescriptor.Kind.ACCESSOR -> {
                descriptorIndex[index] ?:
                    findInTheDescriptorTree(proto)!!
            }
            else -> TODO("Unexpected descriptor kind: $kind")
        }

        return descriptor
    }


    fun deserializeDescriptor(proto: KonanIr.KotlinDescriptor): DeclarationDescriptor {

        context.log{"### deserializeDescriptor ${proto.kind} ${proto.index}"}

        val descriptor = if (proto.hasIrLocalDeclaration()) {
            deserializeLocalDeclaration(proto)
        } else 
            deserializeKnownDescriptor(proto)
        
        descriptorIndex.put(proto.index, descriptor)

        context.log{"### descriptor ${proto.kind} ${proto.index} -> $descriptor"}

        // Now there are several descriptors that automatically
        // recreated in addition to this one. Register them
        // all too.
       
        if (descriptor is FunctionDescriptor) {
            registerParameterDescriptors(proto, descriptor)
        }
        return descriptor
    }


    // --------------------------------------------------------

    // 
    // The section below is a poor man's resolve.
    // Having a 'kind' and a 'name' of a descriptor,
    // we need to find it's original unsubstituted version
    // in the descriptor tree. And then apply the substitutions
    // to get the same substituted descriptor we've obtained
    // in the original ir.
    //

    // We don't deserialize value, type and receiver parameters, rather
    // just obtain them from their function descriptor and match them to the indices
    // stored in the IR serialization.
    fun registerParameterDescriptors (
        proto: KonanIr.KotlinDescriptor, 
        functionDescriptor: FunctionDescriptor) {

        functionDescriptor.valueParameters.forEachIndexed { i, parameter ->
            // Assume they are in the same order.
            val index = proto.getValueParameter(i).index
            descriptorIndex.put(index, parameter)
        }

        functionDescriptor.typeParameters.forEachIndexed {i, parameter ->
            // Assume they are in the same order too.
            val index = proto.getTypeParameter(i).index
            descriptorIndex.put(index, parameter)
        }

        val dispatchReceiver = functionDescriptor.dispatchReceiverParameter
        if (dispatchReceiver != null) {
            assert(proto.hasDispatchReceiverUniqId())
            val dispatchReceiverIndex = proto.dispatchReceiverIndex
            descriptorIndex.put(dispatchReceiverIndex, dispatchReceiver)
        }

        val extensionReceiver = functionDescriptor.extensionReceiverParameter
        if (extensionReceiver != null) {
            assert(proto.hasExtensionReceiverUniqId())
            val extensionReceiverIndex = proto.extensionReceiverIndex
            descriptorIndex.put(extensionReceiverIndex, extensionReceiver)
        }
    }

    fun substituteFunction(proto: KonanIr.KotlinDescriptor, 
        originalDescriptor: FunctionDescriptor): FunctionDescriptor {

        val newDescriptor = 
            originalDescriptor.newCopyBuilder().apply() {
            setOriginal(originalDescriptor)
            setReturnType(deserializeKotlinType(proto.type))
            if (proto.hasExtensionReceiverType()) {
                setExtensionReceiverType(deserializeKotlinType(proto.extensionReceiverType))
            }
        }.build()!!

        descriptorIndex.put(proto.index, newDescriptor)

        return newDescriptor
    }

    fun substituteConstructor(proto: KonanIr.KotlinDescriptor,
        originalDescriptor: ClassConstructorDescriptor): ClassConstructorDescriptor {

        val newDescriptor = originalDescriptor.newCopyBuilder().apply() {
            setOriginal(originalDescriptor)
            setReturnType(deserializeKotlinType(proto.type))
            if (proto.hasExtensionReceiverType()) {
                setExtensionReceiverType(deserializeKotlinType(proto.extensionReceiverType))
            }
        }.build()!!

        descriptorIndex.put(proto.index, newDescriptor)

        // TODO: why?
        return newDescriptor as ClassConstructorDescriptor
    }

    // Property accessors are different because the accessors don't have
    // ProtoBuf serializations. Only their properties do.
    // And also an accessor can not be copied, only properties can.
    fun substituteAccessor(proto: KonanIr.KotlinDescriptor, 
        originalDescriptor: PropertyAccessorDescriptor): PropertyAccessorDescriptor {

        val originalPropertyDescriptor = originalDescriptor.correspondingProperty as DeserializedPropertyDescriptor


        // TODO: property copy building is a mess.
        // Plus it is changing in the big Kotlin under my fingers.
        // What should I do here???
        // how do I set type and extensionreceiver for a property???
        val newPropertyDescriptor = originalPropertyDescriptor

        val newDescriptor = when (originalDescriptor) {
            is PropertyGetterDescriptor ->
                newPropertyDescriptor.getter
            is PropertySetterDescriptor ->
                newPropertyDescriptor.setter
            else -> TODO("Unexpected accessor kind")
        }

        descriptorIndex.put(proto.index, newDescriptor!!)

        newDescriptor.valueParameters.forEachIndexed { i, parameter ->
            // Assume they are in the same order.
            val index = proto.getValueParameter(i).index
            descriptorIndex.put(index, parameter)
        }

        newPropertyDescriptor.typeParameters.forEachIndexed { i, parameter ->
            // Assume they are in the same order too.
            val index = proto.getTypeParameter(i).index
            descriptorIndex.put(index, parameter)
        }

        val dispatchReceiver = newPropertyDescriptor.dispatchReceiverParameter
        if (dispatchReceiver != null) {
            assert(proto.hasDispatchReceiverUniqId())
            val dispatchReceiverIndex = proto.dispatchReceiverIndex
            descriptorIndex.put(dispatchReceiverIndex, dispatchReceiver)
        }

        val extensionReceiver = newPropertyDescriptor.extensionReceiverParameter
        if (extensionReceiver != null) {
            assert(proto.hasExtensionReceiverUniqId())
            val extensionReceiverIndex = proto.extensionReceiverIndex
            descriptorIndex.put(extensionReceiverIndex, extensionReceiver)
        }

        return newDescriptor 
    }

    fun parentMemberScopeByFqNameIndex(index: Int): MemberScope {
        val parent = parentByFqNameIndex(index)
        return when (parent) {
            is ClassDescriptor -> parent.getUnsubstitutedMemberScope()
            is PackageFragmentDescriptor -> parent.getMemberScope()
            is PackageViewDescriptor -> parent.memberScope
            else -> TODO("could not get a member scope")
        }
    }

    fun parentByFqNameIndex(index: Int): DeclarationDescriptor {
        val module = context.moduleDescriptor
        val parent = nameResolver.getDescriptorByFqNameIndex(module, nameTable, index)
        return parent

    }

    fun matchNameInParentScope(proto: KonanIr.KotlinDescriptor): Collection<DeclarationDescriptor> {
        val classOrPackage = proto.classOrPackage
        val name = proto.name

        when (proto.kind) {
            KotlinDescriptor.Kind.CLASS -> {
                val parentScope = 
                    parentMemberScopeByFqNameIndex(classOrPackage)
                val clazz = parentScope.getContributedClassifier(
                    Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                return listOf(clazz!!)

            }
            KotlinDescriptor.Kind.CONSTRUCTOR -> {
                val parent = parentByFqNameIndex(classOrPackage)
                assert(parent is ClassDescriptor)
                return (parent as ClassDescriptor).constructors
            }
            KotlinDescriptor.Kind.ACCESSOR, 
            KotlinDescriptor.Kind.FUNCTION -> {
                val parentScope = 
                    parentMemberScopeByFqNameIndex(classOrPackage)
                return parentScope.contributedMethods.filter{
                    it.name == Name.guessByFirstCharacter(name)
                }
            }
            else -> TODO("Can't find matching names for ${proto.kind}")
        }
    }

    fun selectFunction(
        functions: Collection<DeclarationDescriptor>,
        descriptorProto: KonanIr.KotlinDescriptor):
        DeserializedSimpleFunctionDescriptor {

        val originalIndex = descriptorProto.originalIndex
        val match = functions.singleOrNull() {
            it.uniqId == originalIndex
        } as? DeserializedSimpleFunctionDescriptor
        if (match != null) return match

        // Special case: for invoke we need to re-synthesize 
        // the invoke() descriptor.
        val invoke = functions.singleOrNull {
            (it as FunctionDescriptor).isFunctionInvoke
        }

        if (invoke != null) 
                return createSynthesizedInvokes(listOf(invoke as FunctionDescriptor)).single() as DeserializedSimpleFunctionDescriptor
        else {
            error("Could not find matching descriptor")
        }
    }

    fun selectConstructor(
        constructors: Collection<DeclarationDescriptor>,
        descriptorProto: KonanIr.KotlinDescriptor): 
        DeserializedClassConstructorDescriptor {

        val originalIndex = descriptorProto.originalIndex
        return constructors.single {
            it.uniqId == originalIndex
        } as DeserializedClassConstructorDescriptor
    }


    fun selectAccessor(
        functions: Collection<DeclarationDescriptor>,
        proto: KonanIr.KotlinDescriptor): 
        PropertyAccessorDescriptor {

        // TODO: property accessors are not serialized by
        // descriptor serialization mechanisms, we can't match
        // the "original" field of the deserialized descriptors.
        // So KonanIr.KonanDescriptor.original contains
        // original index of the property rather than accessor's.
        val originalIndex = proto.originalIndex
        return functions.single {
            val property = (it as PropertyAccessorDescriptor).correspondingProperty as DeserializedPropertyDescriptor
            property.uniqId == originalIndex
        } as PropertyAccessorDescriptor
    }

    fun selectAmongMatchingNames(
        matching: Collection<DeclarationDescriptor>, 
        proto: KonanIr.KotlinDescriptor): 
        DeclarationDescriptor {

        return when(proto.kind) {
            KotlinDescriptor.Kind.FUNCTION -> 
                selectFunction(matching, proto)
            KotlinDescriptor.Kind.ACCESSOR -> 
                selectAccessor(matching, proto)
            KotlinDescriptor.Kind.CONSTRUCTOR -> 
                selectConstructor(matching, proto)
            KotlinDescriptor.Kind.CLASS ->
                matching.single()
            else -> TODO("don't know how to select ${proto.kind}")
        }
    }

    fun performSubstitutions(proto: KonanIr.KotlinDescriptor, 
        originalDescriptor: DeclarationDescriptor):
        DeclarationDescriptor {

        //TODO: We need to properly skip creating substituted
        // copies for public descriptors if we know the substituted
        // descriptor is going to be the same.
        // But that's not a trivial thing to find out.
        if (originalDescriptor == rootDescriptor) 
            return rootDescriptor

        return when (originalDescriptor) {
            is SimpleFunctionDescriptor ->
                substituteFunction(proto, originalDescriptor)
            is PropertyAccessorDescriptor ->
                substituteAccessor(proto, originalDescriptor)
            is ClassConstructorDescriptor ->
                substituteConstructor(proto, originalDescriptor)
            is ClassDescriptor ->
                // TODO: do we really need to ever substitute
                // class descriptors here?
                //substituteClass(proto, originalDescriptor)
                originalDescriptor
            else -> TODO("unexpected type of public function")
        }
    }

    fun findInTheDescriptorTree(proto: KonanIr.KotlinDescriptor): DeclarationDescriptor? {

        val matchingNames = matchNameInParentScope(proto).filter{it.isExported()}
        if (matchingNames.size == 0) return null

        val originalDescriptor = selectAmongMatchingNames(matchingNames, proto)

        return performSubstitutions(proto, originalDescriptor)
    }
}

