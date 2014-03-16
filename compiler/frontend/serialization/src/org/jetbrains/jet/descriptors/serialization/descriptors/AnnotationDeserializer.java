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
import org.jetbrains.jet.lang.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;

import static org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers.AnnotatedCallableKind;

public interface AnnotationDeserializer {
    AnnotationDeserializer UNSUPPORTED = new AnnotationDeserializer() {
        @NotNull
        @Override
        public Annotations loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
            return notSupported();
        }

        @NotNull
        @Override
        public Annotations loadCallableAnnotations(
                @NotNull ClassOrPackageFragmentDescriptor container,
                @NotNull ProtoBuf.Callable proto,
                @NotNull NameResolver nameResolver,
                @NotNull AnnotatedCallableKind kind
        ) {
            return notSupported();
        }

        @NotNull
        @Override
        public Annotations loadValueParameterAnnotations(
                @NotNull ClassOrPackageFragmentDescriptor container,
                @NotNull ProtoBuf.Callable callable,
                @NotNull NameResolver nameResolver,
                @NotNull AnnotatedCallableKind kind,
                @NotNull ProtoBuf.Callable.ValueParameter proto
        ) {
            return notSupported();
        }

        @NotNull
        private Annotations notSupported() {
            throw new UnsupportedOperationException("Annotations are not supported");
        }
    };

    @NotNull
    Annotations loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto);

    @NotNull
    Annotations loadCallableAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    );

    @NotNull
    Annotations loadValueParameterAnnotations(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable callable,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind,
            @NotNull ProtoBuf.Callable.ValueParameter proto
    );
}
