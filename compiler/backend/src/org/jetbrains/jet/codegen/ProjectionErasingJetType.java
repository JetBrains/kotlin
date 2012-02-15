/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;

import java.util.ArrayList;
import java.util.List;

/**
 * @author alex.tkachman
 *
 * Utility class used by back-end to
 */
public class ProjectionErasingJetType implements JetType {
    private final JetType delegate;
    private List<TypeProjection> arguments;

    public ProjectionErasingJetType(JetType delegate) {
        this.delegate = delegate;
        arguments = new ArrayList<TypeProjection>();
        for(TypeProjection tp : delegate.getArguments()) {
            arguments.add(new TypeProjection(Variance.INVARIANT, tp.getType()));
        }
    }

    @NotNull
    @Override
    public TypeConstructor getConstructor() {
        return delegate.getConstructor();
    }

    @NotNull
    @Override
    public List<TypeProjection> getArguments() {
        return arguments;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return delegate.getMemberScope();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return delegate.getAnnotations();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof ProjectionErasingJetType) && delegate.equals(((ProjectionErasingJetType)obj).delegate)) || delegate.equals(obj);
    }
}
