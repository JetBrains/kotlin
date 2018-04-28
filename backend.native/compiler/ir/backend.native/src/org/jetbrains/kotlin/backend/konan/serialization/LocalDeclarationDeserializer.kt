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

import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.descriptors.allContainingDeclarations
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirementTable
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

// This class knows how to construct contexts for 
// MemberDeserializer to deserialize descriptors declared in IR.
// Eventually, these descriptors shall be reconstructed from IR declarations,
// or may be just go away completely.

class LocalDeclarationDeserializer(val rootDescriptor: DeclarationDescriptor) {

    val tower: List<DeclarationDescriptor> = 
        (listOf(rootDescriptor) + rootDescriptor.allContainingDeclarations()).reversed()
    init {
        assert(tower.first() is ModuleDescriptor)
    }
    val parents = tower.drop(1)

    val pkg = parents.first() as KonanPackageFragment
    val components = pkg.components
    val nameTable = pkg.proto.nameTable
    val nameResolver = NameResolverImpl(pkg.proto.stringTable, nameTable)
  
    val contextStack = mutableListOf<Pair<DeserializationContext, MemberDeserializer>>()

    init {
        parents.forEach{
            pushContext(it)
        }
    }

    val parentContext: DeserializationContext
        get() = contextStack.peek()!!.first

    val parentTypeTable: TypeTable
        get() = parentContext.typeTable

    val typeDeserializer: TypeDeserializer
        get() = parentContext.typeDeserializer

    val memberDeserializer: MemberDeserializer
        get() = contextStack.peek()!!.second

    fun newContext(descriptor: DeclarationDescriptor): DeserializationContext {
        if (descriptor is KonanPackageFragment) {
            val packageTypeTable = TypeTable(pkg.proto.getPackage().typeTable)
            return components.createContext(
                pkg, nameResolver, packageTypeTable, VersionRequirementTable.EMPTY, null)
        }

        // Only packages and classes have their type tables.
        val typeTable = if (descriptor is DeserializedClassDescriptor) {
            TypeTable(descriptor.classProto.typeTable)
        } else {
            parentTypeTable
        }


        val oldContext = contextStack.peek()!!.first

        return oldContext.childContext(descriptor, 
            descriptor.typeParameterProtos, nameResolver, typeTable)
    }

    fun pushContext(descriptor: DeclarationDescriptor) {
        val newContext = newContext(descriptor)
        contextStack.push(Pair(newContext, MemberDeserializer(newContext)))
    }

    fun popContext(descriptor: DeclarationDescriptor) {
        assert(contextStack.peek()!!.first.containingDeclaration == descriptor)
        contextStack.pop()
    }

    fun deserializeInlineType(type: ProtoBuf.Type) = typeDeserializer.type(type)

    fun deserializeClass(irProto: KonanIr.KotlinDescriptor): ClassDescriptor {
        return DeserializedClassDescriptor(parentContext, irProto.irLocalDeclaration.descriptor.clazz, nameResolver, SourceElement.NO_SOURCE)


    }

    fun deserializeFunction(irProto: KonanIr.KotlinDescriptor): FunctionDescriptor =
        memberDeserializer.loadFunction(irProto.irLocalDeclaration.descriptor.function)

    fun deserializeConstructor(irProto: KonanIr.KotlinDescriptor): ConstructorDescriptor {

       val proto = irProto.irLocalDeclaration.descriptor.constructor
       val isPrimary = !Flags.IS_SECONDARY.get(proto.flags)

        return memberDeserializer.loadConstructor(proto, isPrimary)
    }

    fun deserializeProperty(irProto: KonanIr.KotlinDescriptor): VariableDescriptor {

        val proto = irProto.irLocalDeclaration.descriptor.property
        val property = memberDeserializer.loadProperty(proto)

        return if (proto.getExtension(KonanLinkData.usedAsVariable)) {
            propertyToVariable(property)
        } else {
            property
        }
    }

    private fun propertyToVariable(property: PropertyDescriptor): LocalVariableDescriptor {
        // TODO: Should we transform the getter and the setter too?
        @Suppress("DEPRECATION")
        return LocalVariableDescriptor(
            property.containingDeclaration,
            property.annotations,
            property.name,
            property.type,
            property.isVar,
            property.isDelegated,
            SourceElement.NO_SOURCE)
    }
}


