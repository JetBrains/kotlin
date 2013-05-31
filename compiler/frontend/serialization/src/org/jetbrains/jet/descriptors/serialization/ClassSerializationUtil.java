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
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;

import java.util.Collection;

public class ClassSerializationUtil {
    public interface Sink {
        void writeClass(@NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto);
    }

    public interface SerializerProvider {
        @NotNull
        DescriptorSerializer getSerializerFor(@NotNull ClassDescriptor classDescriptor);
    }

    public static final SerializerProvider NEW_EVERY_TIME = new SerializerProvider() {
        @NotNull
        @Override
        public DescriptorSerializer getSerializerFor(@NotNull ClassDescriptor classDescriptor) {
            return new DescriptorSerializer(NameTable.Namer.DEFAULT);
        }
    };

    @NotNull
    public static SerializerProvider constantSerializer(@NotNull final DescriptorSerializer serializer) {
        return new SerializerProvider() {
            @NotNull
            @Override
            public DescriptorSerializer getSerializerFor(@NotNull ClassDescriptor classDescriptor) {
                return serializer;
            }
        };
    }

    public static void serializeClass(@NotNull ClassDescriptor classDescriptor, @NotNull SerializerProvider serializerProvider, @NotNull Sink sink) {
        DescriptorSerializer serializer = serializerProvider.getSerializerFor(classDescriptor);
        ProtoBuf.Class classProto = serializer.classProto(classDescriptor).build();
        sink.writeClass(classDescriptor, classProto);

        serializeClasses(classDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors(), serializerProvider, sink);

        serializeClasses(classDescriptor.getUnsubstitutedInnerClassesScope().getObjectDescriptors(), serializerProvider, sink);

        ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
        if (classObjectDescriptor != null) {
            serializeClass(classObjectDescriptor, serializerProvider, sink);
        }
    }

    public static void serializeClasses(
            @NotNull Collection<? extends DeclarationDescriptor> descriptors,
            @NotNull SerializerProvider serializerProvider,
            @NotNull Sink sink
    ) {
        for (DeclarationDescriptor descriptor : descriptors) {
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor nestedClass = (ClassDescriptor) descriptor;
                serializeClass(nestedClass, serializerProvider, sink);
            }
        }
    }

    @NotNull
    public static ClassId getClassId(@NotNull ClassDescriptor classDescriptor, @NotNull NameTable.Namer namer) {
        DeclarationDescriptor containingDeclaration = classDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof NamespaceDescriptor) {
            NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) containingDeclaration;
            return new ClassId(getPackageFqName(namespaceDescriptor, namer), FqNameUnsafe.topLevel(namer.getClassName(classDescriptor)));
        }
        // it must be a nested class
        ClassDescriptor outer = (ClassDescriptor) containingDeclaration;
        ClassId outerId = getClassId(outer, namer);
        return outerId.createNestedClassId(namer.getClassName(classDescriptor));
    }

    @NotNull
    public static FqName getPackageFqName(@NotNull NamespaceDescriptor namespaceDescriptor, @NotNull NameTable.Namer namer) {
        if (DescriptorUtils.isRootNamespace(namespaceDescriptor)) return FqName.ROOT;

        NamespaceDescriptor parent = (NamespaceDescriptor) namespaceDescriptor.getContainingDeclaration();
        return getPackageFqName(parent, namer)
                .child(namer.getPackageName(namespaceDescriptor));
    }
}
