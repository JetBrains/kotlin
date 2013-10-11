/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.descriptors.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;

import java.util.Collection;

public class ClassSerializationUtil {
    private ClassSerializationUtil() {
    }

    public interface Sink {
        void writeClass(@NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto);
    }

    private static void serializeClass(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull DescriptorSerializer serializer,
            @NotNull Sink sink
    ) {
        ProtoBuf.Class classProto = serializer.classProto(classDescriptor).build();
        sink.writeClass(classDescriptor, classProto);

        serializeClasses(classDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors(), serializer, sink);

        ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
        if (classObjectDescriptor != null) {
            serializeClass(classObjectDescriptor, serializer, sink);
        }
    }

    public static void serializeClasses(
            @NotNull Collection<? extends DeclarationDescriptor> descriptors,
            @NotNull DescriptorSerializer serializer,
            @NotNull Sink sink
    ) {
        for (DeclarationDescriptor descriptor : descriptors) {
            if (descriptor instanceof ClassDescriptor) {
                serializeClass((ClassDescriptor) descriptor, serializer, sink);
            }
        }
    }

    @NotNull
    public static ClassId getClassId(@NotNull ClassDescriptor classDescriptor) {
        DeclarationDescriptor owner = classDescriptor.getContainingDeclaration();
        if (owner instanceof PackageFragmentDescriptor) {
            return new ClassId(((PackageFragmentDescriptor) owner).getFqName(), FqNameUnsafe.topLevel(classDescriptor.getName()));
        }
        return getClassId((ClassDescriptor) owner).createNestedClassId(classDescriptor.getName());
    }
}
