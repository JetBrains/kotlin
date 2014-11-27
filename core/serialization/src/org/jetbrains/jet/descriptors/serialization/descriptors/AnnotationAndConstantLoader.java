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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;

import java.util.List;

public interface AnnotationAndConstantLoader {
    AnnotationAndConstantLoader UNSUPPORTED = new AnnotationAndConstantLoader() {
        @NotNull
        @Override
        public List<AnnotationDescriptor> loadClassAnnotations(
                @NotNull ProtoBuf.Class classProto,
                @NotNull NameResolver nameResolver
        ) {
            return annotationsNotSupported();
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadCallableAnnotations(
                @NotNull ProtoContainer container,
                @NotNull ProtoBuf.Callable proto,
                @NotNull NameResolver nameResolver,
                @NotNull AnnotatedCallableKind kind
        ) {
            return annotationsNotSupported();
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadValueParameterAnnotations(
                @NotNull ProtoContainer container,
                @NotNull ProtoBuf.Callable callable,
                @NotNull NameResolver nameResolver,
                @NotNull AnnotatedCallableKind kind,
                @NotNull ProtoBuf.Callable.ValueParameter proto
        ) {
            return annotationsNotSupported();
        }

        @Nullable
        @Override
        public CompileTimeConstant<?> loadPropertyConstant(
                @NotNull ProtoContainer container,
                @NotNull ProtoBuf.Callable proto,
                @NotNull NameResolver nameResolver,
                @NotNull AnnotatedCallableKind kind
        ) {
            throw new UnsupportedOperationException("Constants are not supported");
        }

        @NotNull
        private List<AnnotationDescriptor> annotationsNotSupported() {
            throw new UnsupportedOperationException("Annotations are not supported");
        }
    };

    @NotNull
    List<AnnotationDescriptor> loadClassAnnotations(
            @NotNull ProtoBuf.Class classProto,
            @NotNull NameResolver nameResolver
    );

    @NotNull
    List<AnnotationDescriptor> loadCallableAnnotations(
            @NotNull ProtoContainer container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    );

    @NotNull
    List<AnnotationDescriptor> loadValueParameterAnnotations(
            @NotNull ProtoContainer container,
            @NotNull ProtoBuf.Callable callable,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind,
            @NotNull ProtoBuf.Callable.ValueParameter proto
    );

    @Nullable
    CompileTimeConstant<?> loadPropertyConstant(
            @NotNull ProtoContainer container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    );
}
