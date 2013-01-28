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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.List;

/**
 * This is a fake type assigned to namespace expressions. Only member lookup is
 * supposed to be done on these types.
 */
public class NamespaceType implements JetType {
    private final Name name;
    @NotNull
    private final JetScope memberScope;

    public NamespaceType(@NotNull Name name, @NotNull JetScope memberScope) {
        this.name = name;
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public TypeConstructor getConstructor() {
        return throwException();
    }

    private TypeConstructor throwException() {
        throw new UnsupportedOperationException("Only member lookup is allowed on a namespace type " + name);
    }

    @NotNull
    @Override
    public List<TypeProjection> getArguments() {
        throwException();
        return null;
    }

    @Override
    public boolean isNullable() {
        throwException();
        return false;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        throwException();
        return null;
    }
}
