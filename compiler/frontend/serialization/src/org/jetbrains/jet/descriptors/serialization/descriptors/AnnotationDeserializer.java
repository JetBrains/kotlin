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

package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;

import java.util.List;

public interface AnnotationDeserializer {
    AnnotationDeserializer UNSUPPORTED = new AnnotationDeserializer() {
        @NotNull
        @Override
        public List<AnnotationDescriptor> loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
            return notSupported();
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadCallableAnnotations(
                @NotNull ClassOrNamespaceDescriptor container,
                @NotNull ProtoBuf.Callable proto,
                @NotNull NameResolver nameResolver,
                @NotNull AnnotatedCallableKind kind
        ) {
            return notSupported();
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadValueParameterAnnotations(
                @NotNull ClassOrNamespaceDescriptor container,
                @NotNull ProtoBuf.Callable callable,
                @NotNull NameResolver nameResolver,
                @NotNull ProtoBuf.Callable.ValueParameter proto
        ) {
            return notSupported();
        }

        @NotNull
        private List<AnnotationDescriptor> notSupported() {
            throw new UnsupportedOperationException("Annotations are not supported");
        }
    };

    enum AnnotatedCallableKind {
        FUNCTION,
        PROPERTY,
        PROPERTY_GETTER,
        PROPERTY_SETTER
    }

    @NotNull
    List<AnnotationDescriptor> loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto);

    @NotNull
    List<AnnotationDescriptor> loadCallableAnnotations(
            @NotNull ClassOrNamespaceDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    );

    @NotNull
    List<AnnotationDescriptor> loadValueParameterAnnotations(
            @NotNull ClassOrNamespaceDescriptor container,
            @NotNull ProtoBuf.Callable callable,
            @NotNull NameResolver nameResolver,
            @NotNull ProtoBuf.Callable.ValueParameter proto
    );
}
