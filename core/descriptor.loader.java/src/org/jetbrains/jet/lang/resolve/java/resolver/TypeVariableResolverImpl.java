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

package org.jetbrains.jet.lang.resolve.java.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

/* package */ class TypeVariableResolverImpl implements TypeVariableResolver {
    @NotNull
    private final List<TypeParameterDescriptor> typeParameters;
    @NotNull
    private final DeclarationDescriptor owner;

    public TypeVariableResolverImpl(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull DeclarationDescriptor owner) {
        this.typeParameters = typeParameters;
        this.owner = owner;

        assert parametersBelongToOwner() : "Type parameters should belong to owner: " + owner + "; " + typeParameters;
    }

    private boolean parametersBelongToOwner() {
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            if (typeParameter.getContainingDeclaration() != owner) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public TypeParameterDescriptor getTypeVariable(@NotNull Name name) {
        return getTypeVariable(name, typeParameters, owner);
    }

    @Nullable
    private static TypeParameterDescriptor getTypeVariable(
            @NotNull Name name,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull DeclarationDescriptor owner
    ) {
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            if (typeParameter.getName().equals(name)) {
                return typeParameter;
            }
        }

        DeclarationDescriptor container = owner.getContainingDeclaration();
        if (container instanceof ClassDescriptor) {
            return getTypeVariable(name, ((ClassDescriptor) container).getTypeConstructor().getParameters(), container);
        }

        return null;
    }
}
