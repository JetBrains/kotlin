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

package org.jetbrains.jet.lang.resolve.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantDeserializer;
import org.jetbrains.jet.lang.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;

import javax.inject.Inject;

import static org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers.AnnotatedCallableKind;
import static org.jetbrains.jet.lang.resolve.kotlin.DescriptorDeserializersStorage.MemberSignature;

public class ConstantDescriptorDeserializer extends BaseDescriptorDeserializer implements ConstantDeserializer {
    @Inject
    @Override
    public void setStorage(@NotNull DescriptorDeserializersStorage storage) {
        this.storage = storage;
    }

    @Inject
    @Override
    public void setClassResolver(@NotNull DependencyClassByQualifiedNameResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    @Override
    public void setKotlinClassFinder(@NotNull KotlinClassFinder kotlinClassFinder) {
        this.kotlinClassFinder = kotlinClassFinder;
    }

    @Inject
    @Override
    public void setErrorReporter(@NotNull ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Nullable
    @Override
    public CompileTimeConstant<?> loadPropertyConstant(
            @NotNull ClassOrPackageFragmentDescriptor container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        MemberSignature signature = getCallableSignature(proto, nameResolver, kind);
        if (signature == null) return null;

        KotlinJvmBinaryClass kotlinClass = findClassWithMemberAnnotations(container, proto, nameResolver, kind);
        if (kotlinClass == null) {
            errorReporter.reportAnnotationLoadingError("Kotlin class for loading property constant is not found: " + container, null);
            return null;
        }

        return storage.getStorage().invoke(kotlinClass).getPropertyConstants().get(signature);
    }
}
