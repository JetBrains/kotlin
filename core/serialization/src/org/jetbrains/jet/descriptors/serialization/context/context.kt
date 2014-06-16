/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.descriptors.serialization.context

import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.descriptors.serialization.DescriptorFinder
import org.jetbrains.jet.descriptors.serialization.NameResolver
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantDeserializer
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.descriptors.serialization.TypeDeserializer
import org.jetbrains.jet.descriptors.serialization.DescriptorDeserializer
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.TypeParameter
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.jet.descriptors.serialization.TypeDeserializer.TypeParameterResolver

public open class DeserializationGlobalContext(
        public val storageManager: StorageManager,
        public val descriptorFinder: DescriptorFinder,
        public val annotationDeserializer: AnnotationDeserializer,
        public val constantDeserializer: ConstantDeserializer,
        public val packageFragmentProvider: PackageFragmentProvider,
        public val memberFilter: MemberFilter
) {
    public fun withNameResolver(nameResolver: NameResolver): DeserializationContext {
        return DeserializationContext(storageManager, descriptorFinder, annotationDeserializer,
                                      constantDeserializer, packageFragmentProvider, memberFilter, nameResolver)
    }
}


public open class DeserializationContext(
        storageManager: StorageManager,
        descriptorFinder: DescriptorFinder,
        annotationDeserializer: AnnotationDeserializer,
        constantDeserializer: ConstantDeserializer,
        packageFragmentProvider: PackageFragmentProvider,
        memberFilter: MemberFilter,
        public val nameResolver: NameResolver
) : DeserializationGlobalContext(storageManager, descriptorFinder, annotationDeserializer,
                                 constantDeserializer, packageFragmentProvider, memberFilter) {
    fun withTypes(containingDeclaration: DeclarationDescriptor): DeserializationContextWithTypes {
        val typeDeserializer = TypeDeserializer(this, null, "Deserializer for ${containingDeclaration.getName()}",
                                                TypeDeserializer.TypeParameterResolver.NONE)
        return withTypes(containingDeclaration, typeDeserializer)
    }

    fun withTypes(containingDeclaration: DeclarationDescriptor, typeDeserializer: TypeDeserializer): DeserializationContextWithTypes {
        return DeserializationContextWithTypes(storageManager, descriptorFinder, annotationDeserializer,
                                               constantDeserializer, packageFragmentProvider, memberFilter,
                                               nameResolver, containingDeclaration,
                                               typeDeserializer)
    }

}


class DeserializationContextWithTypes(
        storageManager: StorageManager,
        descriptorFinder: DescriptorFinder,
        annotationDeserializer: AnnotationDeserializer,
        constantDeserializer: ConstantDeserializer,
        packageFragmentProvider: PackageFragmentProvider,
        memberFilter: MemberFilter,
        nameResolver: NameResolver,
        val containingDeclaration: DeclarationDescriptor,
        val typeDeserializer: TypeDeserializer
) : DeserializationContext(storageManager, descriptorFinder, annotationDeserializer,
                           constantDeserializer, packageFragmentProvider, memberFilter, nameResolver) {
    val deserializer: DescriptorDeserializer = DescriptorDeserializer(this)

    public fun childContext(
            descriptor: DeclarationDescriptor,
            typeParameterProtos: List<TypeParameter>,
            typeParameters: MutableList<TypeParameterDescriptor>
    ): DeserializationContextWithTypes {
        val childTypeParameterResolver = object : TypeDeserializer.TypeParameterResolver {
            override fun getTypeParameters(typeDeserializer: TypeDeserializer): List<DeserializedTypeParameterDescriptor> {
                val descriptors = deserializer.typeParameters(typeParameterProtos, typeDeserializer)
                typeParameters.addAll(descriptors)
                return descriptors
            }
        }
        val childTypeDeserializer = TypeDeserializer(this, typeDeserializer, "Child deserializer for " + descriptor.getName(),
                                                     childTypeParameterResolver)
        return withTypes(descriptor, childTypeDeserializer)
    }

}
