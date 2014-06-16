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
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationLoader
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.descriptors.serialization.DescriptorFinder
import org.jetbrains.jet.descriptors.serialization.NameResolver
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.descriptors.serialization.TypeDeserializer
import org.jetbrains.jet.descriptors.serialization.MemberDeserializer
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.TypeParameter
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantLoader
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.descriptors.serialization.ClassId
import org.jetbrains.jet.descriptors.serialization.ClassDeserializer

public open class DeserializationGlobalContext(
        public val storageManager: StorageManager,
        public val moduleDescriptor: ModuleDescriptor,
        public val descriptorFinder: DescriptorFinder,
        public val annotationLoader: AnnotationLoader,
        public val constantLoader: ConstantLoader,
        public val packageFragmentProvider: PackageFragmentProvider,
        public val classDeserializer: ClassDeserializer = ClassDeserializer(storageManager, descriptorFinder)
) {
    public fun withNameResolver(nameResolver: NameResolver): DeserializationContext {
        return DeserializationContext(storageManager, moduleDescriptor, descriptorFinder, annotationLoader,
                                      constantLoader, packageFragmentProvider, classDeserializer, nameResolver)
    }
}


public open class DeserializationContext(
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        descriptorFinder: DescriptorFinder,
        annotationLoader: AnnotationLoader,
        constantLoader: ConstantLoader,
        packageFragmentProvider: PackageFragmentProvider,
        classDeserializer: ClassDeserializer,
        public val nameResolver: NameResolver
) : DeserializationGlobalContext(storageManager, moduleDescriptor, descriptorFinder, annotationLoader,
                                 constantLoader, packageFragmentProvider, classDeserializer) {
    fun withTypes(containingDeclaration: DeclarationDescriptor): DeserializationContextWithTypes {
        val typeDeserializer = TypeDeserializer(this, null, "Deserializer for ${containingDeclaration.getName()}",
                                                TypeDeserializer.TypeParameterResolver.NONE)
        return withTypes(containingDeclaration, typeDeserializer)
    }

    fun withTypes(containingDeclaration: DeclarationDescriptor, typeDeserializer: TypeDeserializer): DeserializationContextWithTypes {
        return DeserializationContextWithTypes(storageManager, moduleDescriptor, descriptorFinder, annotationLoader,
                                               constantLoader, packageFragmentProvider, classDeserializer,
                                               nameResolver, containingDeclaration, typeDeserializer)
    }

}


class DeserializationContextWithTypes(
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        descriptorFinder: DescriptorFinder,
        annotationLoader: AnnotationLoader,
        constantLoader: ConstantLoader,
        packageFragmentProvider: PackageFragmentProvider,
        classDeserializer: ClassDeserializer,
        nameResolver: NameResolver,
        val containingDeclaration: DeclarationDescriptor,
        val typeDeserializer: TypeDeserializer
) : DeserializationContext(storageManager, moduleDescriptor, descriptorFinder, annotationLoader,
                           constantLoader, packageFragmentProvider, classDeserializer, nameResolver) {
    val deserializer: MemberDeserializer = MemberDeserializer(this)

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

fun DeserializationGlobalContext.deserializeClass(classId: ClassId): ClassDescriptor? {
    return classDeserializer.deserializeClass(classId)
}
