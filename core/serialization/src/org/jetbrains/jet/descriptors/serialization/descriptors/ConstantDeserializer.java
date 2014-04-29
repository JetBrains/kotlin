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

package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;

import static org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers.AnnotatedCallableKind;

public interface ConstantDeserializer {
    ConstantDeserializer UNSUPPORTED = new ConstantDeserializer() {
        @Nullable
        @Override
        public CompileTimeConstant<?> loadPropertyConstant(
                @NotNull ClassOrPackageFragmentDescriptor container,
                @NotNull ProtoBuf.Callable proto,
                @NotNull NameResolver nameResolver,
                @NotNull AnnotatedCallableKind kind
        ) {
            throw new UnsupportedOperationException("Constants are not supported");
        }
    };

    @Nullable
    CompileTimeConstant<?> loadPropertyConstant(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    );
}
