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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.KonanIr
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfoTable
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.types.KotlinType

// This class knows how to construct contexts for 
// MemberDeserializer to deserialize descriptors declared in IR.
// Consider merging it all to the very MemberDesereializer itself eventually.

class LocalDeclarationDeserializer(val rootDescriptor: DeserializedCallableMemberDescriptor, val module: ModuleDescriptor) {
        
    val pkg = rootDescriptor.getContainingDeclaration() as KonanPackageFragment
    init {
        assert(pkg is KonanPackageFragment)
    }

    val components = pkg.components
    val nameResolver = NameResolverImpl(pkg.proto.getStringTable(), pkg.proto.getNameTable())
    val nameTable = pkg.proto.getNameTable()
    val typeTable = TypeTable(pkg.proto.getPackage().getTypeTable())
    val context = components.createContext(
        pkg, nameResolver, typeTable, SinceKotlinInfoTable.EMPTY, null)

    val typeParameterProtos = when (rootDescriptor) {
        // These are two different typeParameterLists not 
        // having a common ancestor.
        is DeserializedSimpleFunctionDescriptor
            -> rootDescriptor.proto.typeParameterList
        is DeserializedPropertyDescriptor
            -> rootDescriptor.proto.typeParameterList
        else -> error("Unexpected descriptor kind")
    }

    val childContext = context.childContext(rootDescriptor, typeParameterProtos, nameResolver, typeTable)
    val typeDeserializer = childContext.typeDeserializer

    val memberDeserializer = MemberDeserializer(childContext)

    fun deserializeInlineType(type: ProtoBuf.Type): KotlinType {
        val result = typeDeserializer.type(type)

        return result
    }

    fun getDescriptorByFqNameIndex(module: ModuleDescriptor, fqNameIndex: Int): DeclarationDescriptor {

        val packageName = nameResolver.getPackageFqName(fqNameIndex)
        // Here we using internals of NameresolverImpl. 
        val proto = nameTable.getQualifiedName(fqNameIndex)
        when (proto.kind) {
            QualifiedName.Kind.CLASS,
            QualifiedName.Kind.LOCAL ->
                return module.findClassAcrossModuleDependencies(nameResolver.getClassId(fqNameIndex))!!
            QualifiedName.Kind.PACKAGE ->
                return module.getPackage(packageName)
            else -> TODO("Unexpected descriptor kind.")
        }
    }

    fun memberDeserializerByParentFqNameIndex(fqNameIndex: Int): MemberDeserializer {

       val parent = getDescriptorByFqNameIndex(module, fqNameIndex)
       val typeParameters = if (parent is DeserializedClassDescriptor) {
           parent.classProto.typeParameterList
       } else listOf<ProtoBuf.TypeParameter>()

       val childContext = context.childContext(parent, typeParameters, nameResolver, typeTable)
       return MemberDeserializer(childContext)

    }

    private fun memberDeserializer(irProto: KonanIr.KotlinDescriptor): MemberDeserializer {
        return if (irProto.hasClassOrPackage()) {
            memberDeserializerByParentFqNameIndex(irProto.classOrPackage)
        } else {
            // TODO: learn to take the containing IR declaration
            this.memberDeserializer
        }
    }

    fun deserializeFunction(irProto: KonanIr.KotlinDescriptor): FunctionDescriptor =
        memberDeserializer(irProto).loadFunction(irProto.irLocalDeclaration.descriptor.function)

    fun deserializeConstructor(irProto: KonanIr.KotlinDescriptor): ConstructorDescriptor {

       val proto = irProto.irLocalDeclaration.descriptor.constructor
       val memberDeserializer = memberDeserializerByParentFqNameIndex(irProto.classOrPackage)
       val isPrimary = !Flags.IS_SECONDARY.get(proto.flags)
       val constructor = memberDeserializer.loadConstructor(proto, isPrimary)

       return constructor
    }

    fun deserializeProperty(irProto: KonanIr.KotlinDescriptor): VariableDescriptor {

        val proto = irProto.irLocalDeclaration.descriptor.property
        val property = memberDeserializer(irProto).loadProperty(proto)

        return if (proto.getExtension(KonanLinkData.usedAsVariable)) {
            propertyToVariable(property)
        } else {
            property
        }
    }

    fun propertyToVariable(property: PropertyDescriptor): LocalVariableDescriptor {
        val variable = LocalVariableDescriptor(
            property.containingDeclaration,
            property.annotations,
            property.name,
            property.type,
            property.isVar, 
            property.isDelegated,
            SourceElement.NO_SOURCE)

        // TODO: Should we transform the getter and the setter too?
        return variable
    }
}


