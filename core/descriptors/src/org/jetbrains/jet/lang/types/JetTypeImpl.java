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

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collections;
import java.util.List;

public final class JetTypeImpl extends AbstractJetType {

    private final TypeConstructor constructor;
    private final List<? extends TypeProjection> arguments;
    private final boolean nullable;
    private final JetScope memberScope;
    private final List<AnnotationDescriptor> annotations;

    public JetTypeImpl(List<AnnotationDescriptor> annotations, TypeConstructor constructor, boolean nullable, @NotNull List<? extends TypeProjection> arguments, JetScope memberScope) {
        this.annotations = annotations;

        if (memberScope instanceof ErrorUtils.ErrorScope) {
            throw new IllegalStateException("JetTypeImpl should not be created for error type: " + memberScope + "\n" + constructor);
        }

        this.constructor = constructor;
        this.nullable = nullable;
        this.arguments = arguments;
        this.memberScope = memberScope;
    }

    public JetTypeImpl(TypeConstructor constructor, JetScope memberScope) {
        this(Collections.<AnnotationDescriptor>emptyList(), constructor, false, Collections.<TypeProjection>emptyList(), memberScope);
    }

    public JetTypeImpl(@NotNull ClassDescriptor classDescriptor) {
        this(Collections.<AnnotationDescriptor>emptyList(),
                classDescriptor.getTypeConstructor(),
                false,
                Collections.<TypeProjection>emptyList(),
                classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList()));
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations;
    }

    @NotNull
    @Override
    public TypeConstructor getConstructor() {
        return constructor;
    }

    @NotNull
    @Override
    public List<TypeProjection> getArguments() {
        //noinspection unchecked
        return (List) arguments;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @Override
    public boolean isError() {
        return false;
    }
}
