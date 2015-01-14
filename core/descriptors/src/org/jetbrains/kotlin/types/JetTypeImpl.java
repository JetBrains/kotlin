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
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

import java.util.List;

public final class JetTypeImpl extends AbstractJetType {

    private final TypeConstructor constructor;
    private final List<? extends TypeProjection> arguments;
    private final boolean nullable;
    private final JetScope memberScope;
    private final Annotations annotations;

    public JetTypeImpl(
            @NotNull Annotations annotations,
            @NotNull TypeConstructor constructor,
            boolean nullable,
            @NotNull List<? extends TypeProjection> arguments,
            @NotNull JetScope memberScope
    ) {
        this.annotations = annotations;

        if (memberScope instanceof ErrorUtils.ErrorScope) {
            throw new IllegalStateException("JetTypeImpl should not be created for error type: " + memberScope + "\n" + constructor);
        }

        this.constructor = constructor;
        this.nullable = nullable;
        this.arguments = arguments;
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
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
    public boolean isMarkedNullable() {
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
