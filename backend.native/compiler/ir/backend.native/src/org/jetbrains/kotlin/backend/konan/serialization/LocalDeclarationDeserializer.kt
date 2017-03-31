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
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfoTable
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.types.KotlinType

// This class knows how to construct contexts for 
// MemberDeserializer to deserialize descriptors declared in IR.
// Consider merging it all to the very MemberDesereializer itself eventually.

class LocalDeclarationDeserializer(val rootFunction: FunctionDescriptor, val module: ModuleDescriptor) {
        
    val pkg = rootFunction.getContainingDeclaration() as KonanPackageFragment
    init {
        assert(pkg is KonanPackageFragment)
    }

    val components = pkg.components
    val nameResolver = NameResolverImpl(pkg.proto.getStringTable(), pkg.proto.getNameTable())
    val nameTable = pkg.proto.getNameTable()
    val typeTable = TypeTable(pkg.proto.getPackage().getTypeTable())
    val context = components.createContext(
        pkg, nameResolver, typeTable, SinceKotlinInfoTable.EMPTY, null)

    val typeParameters = (rootFunction as DeserializedSimpleFunctionDescriptor).proto.typeParameterList

    val childContext = context.childContext(rootFunction, typeParameters, nameResolver, typeTable)
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

    fun memberDeserializerByParentFqNameIndex(fqName: Int): MemberDeserializer {

       val parent = getDescriptorByFqNameIndex(module, fqName)
       val typeParameters = if (parent is DeserializedClassDescriptor) {
           parent.classProto.typeParameterList
       } else listOf<ProtoBuf.TypeParameter>()

       val childContext = context.childContext(parent, typeParameters, nameResolver, typeTable)
       return MemberDeserializer(childContext)

    }

    fun deserializeFunction(proto: ProtoBuf.Function): FunctionDescriptor {
       val containingFqName = proto.getExtension(KonanLinkData.functionParent)
       // TODO: learn to take the containing IR declaration
       val memberDeserializer = if (containingFqName == -1) 
            this.memberDeserializer
       else
            memberDeserializerByParentFqNameIndex(containingFqName)

       val function = memberDeserializer.loadFunction(proto)

        return function
    }

    fun deserializeConstructor(proto: ProtoBuf.Constructor): ConstructorDescriptor {

       val containingFqName = proto.getExtension(KonanLinkData.constructorParent)
       val memberDeserializer = memberDeserializerByParentFqNameIndex(containingFqName)
       val isPrimary = !Flags.IS_SECONDARY.get(proto.flags)
       val constructor = memberDeserializer.loadConstructor(proto, isPrimary)

       return constructor
    }


    fun deserializeProperty(proto: ProtoBuf.Property): VariableDescriptor {
        val containingFqName = proto.getExtension(KonanLinkData.propertyParent)

        // TODO: learn to take the containing IR declaration
        val memberDeserializer = if (containingFqName == -1) 
            this.memberDeserializer
        else
            memberDeserializerByParentFqNameIndex(containingFqName)

        val property = memberDeserializer.loadProperty(proto)

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


