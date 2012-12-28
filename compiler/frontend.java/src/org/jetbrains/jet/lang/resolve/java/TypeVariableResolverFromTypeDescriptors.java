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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;

public class TypeVariableResolverFromTypeDescriptors implements TypeVariableResolver {

    @NotNull
    private final List<TypeParameterDescriptor> typeParameters;
    @NotNull
    private final DeclarationDescriptor typeParametersOwner;
    @NotNull
    private final String context;

    public TypeVariableResolverFromTypeDescriptors(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull DeclarationDescriptor owner,
            @NotNull String context) {
        this.typeParameters = typeParameters;
        this.typeParametersOwner = owner;
        this.context = context;

        for (TypeParameterDescriptor typeParameter : typeParameters) {
            if (typeParameter.getContainingDeclaration() != owner) {
                throw new IllegalStateException();
            }
        }
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
        return getTypeVariable(name, typeParameters, typeParametersOwner, context);
    }

    @NotNull
    private static TypeParameterDescriptor getTypeVariable(
            @NotNull String name,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull DeclarationDescriptor owner,
            @NotNull String context) {
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            if (typeParameter.getName().getName().equals(name)) {
                return typeParameter;
            }
        }

        DeclarationDescriptor containingDeclaration = owner.getContainingDeclaration();
        if (containingDeclaration != null) {
            return getTypeVariable(
                    name,
                    TypeVariableResolvers.getTypeParameterDescriptors((ClassOrNamespaceDescriptor) containingDeclaration),
                    containingDeclaration,
                    context);
        }
        throw new RuntimeException("type parameter not found by name '" + name + "' in " + context);
    }
}
