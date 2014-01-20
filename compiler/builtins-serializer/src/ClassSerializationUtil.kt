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

package org.jetbrains.jet.utils.builtinsSerializer

import org.jetbrains.jet.descriptors.serialization.ClassId
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe

public object ClassSerializationUtil {
    public trait Sink {
        fun writeClass(classDescriptor: ClassDescriptor, classProto: ProtoBuf.Class)
    }

    private fun serializeClass(classDescriptor: ClassDescriptor, serializer: DescriptorSerializer, sink: Sink) {
        val classProto = serializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
        sink.writeClass(classDescriptor, classProto)

        serializeClasses(classDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors(), serializer, sink)

        val classObjectDescriptor = classDescriptor.getClassObjectDescriptor()
        if (classObjectDescriptor != null) {
            serializeClass(classObjectDescriptor, serializer, sink)
        }
    }

    public fun serializeClasses(descriptors: Collection<DeclarationDescriptor>, serializer: DescriptorSerializer, sink: Sink) {
        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(descriptor, serializer, sink)
            }
        }
    }

    public fun getClassId(classDescriptor: ClassDescriptor): ClassId {
        val owner = classDescriptor.getContainingDeclaration()
        if (owner is PackageFragmentDescriptor) {
            return ClassId(owner.getFqName(), FqNameUnsafe.topLevel(classDescriptor.getName()))
        }
        return getClassId(owner as ClassDescriptor).createNestedClassId(classDescriptor.getName())
    }
}
