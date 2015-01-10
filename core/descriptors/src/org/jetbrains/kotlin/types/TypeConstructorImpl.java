/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class TypeConstructorImpl extends AnnotatedImpl implements TypeConstructor {

    @NotNull
    public static TypeConstructorImpl createForClass(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull Annotations annotations,
            boolean isFinal,
            @NotNull String debugName,
            @NotNull List<? extends TypeParameterDescriptor> parameters,
            @NotNull Collection<JetType> supertypes
    ) {
        return new TypeConstructorImpl(classDescriptor, annotations, isFinal, debugName, parameters, supertypes) {
            @Override
            public int hashCode() {
                return AbstractClassTypeConstructor.hashCode(this);
            }

            @Override
            @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
            public boolean equals(Object obj) {
                return AbstractClassTypeConstructor.equals(this, obj);
            }
        };
    }

    @NotNull
    public static TypeConstructorImpl createForTypeParameter(
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull Annotations annotations,
            boolean isFinal,
            @NotNull String debugName,
            @NotNull List<? extends TypeParameterDescriptor> parameters,
            @NotNull Collection<JetType> supertypes
    ) {
        return new TypeConstructorImpl(typeParameterDescriptor, annotations, isFinal, debugName, parameters, supertypes) {
            @Override
            public int hashCode() {
                return System.identityHashCode(this);
            }

            @Override
            public boolean equals(Object obj) {
                return this == obj;
            }
        };
    }

    private final List<TypeParameterDescriptor> parameters;
    private final Collection<JetType> supertypes;
    private final String debugName;
    private final boolean isFinal;

    private final ClassifierDescriptor classifierDescriptor;

    private TypeConstructorImpl(
            @Nullable ClassifierDescriptor classifierDescriptor,
            @NotNull Annotations annotations,
            boolean isFinal,
            @NotNull String debugName,
            @NotNull List<? extends TypeParameterDescriptor> parameters,
            @NotNull Collection<JetType> supertypes) {
        super(annotations);
        this.classifierDescriptor = classifierDescriptor;
        this.isFinal = isFinal;
        this.debugName = debugName;
        this.parameters = Collections.unmodifiableList(new ArrayList<TypeParameterDescriptor>(parameters));
        this.supertypes = Collections.unmodifiableCollection(supertypes);
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getParameters() {
        return parameters;
    }

    @Override
    @NotNull
    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    @Override
    public String toString() {
        return debugName;
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public boolean isDenotable() {
        return true;
    }

    @Override
    @Nullable
    public ClassifierDescriptor getDeclarationDescriptor() {
        return classifierDescriptor;
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
