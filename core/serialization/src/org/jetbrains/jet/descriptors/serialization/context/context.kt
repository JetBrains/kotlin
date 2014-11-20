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
import org.jetbrains.jet.descriptors.serialization.ClassDataFinder
import org.jetbrains.jet.descriptors.serialization.NameResolver
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.descriptors.serialization.TypeDeserializer
import org.jetbrains.jet.descriptors.serialization.MemberDeserializer
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.TypeParameter
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantLoader
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.descriptors.serialization.ClassDeserializer
import org.jetbrains.jet.descriptors.serialization.FlexibleTypeCapabilitiesDeserializer

public class DeserializationComponents(
        public val storageManager: StorageManager,
        public val moduleDescriptor: ModuleDescriptor,
        public val classDataFinder: ClassDataFinder,
        public val annotationLoader: AnnotationLoader,
        public val constantLoader: ConstantLoader,
        public val packageFragmentProvider: PackageFragmentProvider,
        public val flexibleTypeCapabilitiesDeserializer: FlexibleTypeCapabilitiesDeserializer
) {
    public val classDeserializer: ClassDeserializer = ClassDeserializer(this)

    public fun deserializeClass(classId: ClassId): ClassDescriptor? = classDeserializer.deserializeClass(classId)

    public fun createContext(nameResolver: NameResolver): DeserializationContext = DeserializationContext(this, nameResolver)
}


public open class DeserializationContext(
        public val components: DeserializationComponents,
        public val nameResolver: NameResolver
) {
    fun withTypes(containingDeclaration: DeclarationDescriptor, parent: TypeDeserializer? = null): DeserializationContextWithTypes {
        val typeDeserializer = TypeDeserializer(this, parent, "Deserializer for ${containingDeclaration.getName()}")
        return DeserializationContextWithTypes(components, nameResolver, containingDeclaration, typeDeserializer)
    }
}


class DeserializationContextWithTypes(
        components: DeserializationComponents,
        nameResolver: NameResolver,
        val containingDeclaration: DeclarationDescriptor,
        val typeDeserializer: TypeDeserializer
) : DeserializationContext(components, nameResolver) {
    val memberDeserializer: MemberDeserializer = MemberDeserializer(this)

    fun childContext(descriptor: DeclarationDescriptor, typeParameterProtos: List<TypeParameter>): DeserializationContextWithTypes {
        val child = this.withTypes(descriptor, parent = this.typeDeserializer)

        for (typeParameter in child.memberDeserializer.typeParameters(typeParameterProtos)) {
            child.typeDeserializer.addTypeParameter(typeParameter)
        }

        return child
    }
}
