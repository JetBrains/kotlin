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

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;

/* package */ class TypeVariableResolver {
    @NotNull
    protected final List<TypeParameterDescriptor> typeParameters;
    @NotNull
    protected final DeclarationDescriptor owner;
    @NotNull
    protected final String context;

    public TypeVariableResolver(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull DeclarationDescriptor owner,
            @NotNull String context
    ) {
        this.typeParameters = typeParameters;
        this.owner = owner;
        this.context = context;

        assert ContainerUtil.and(typeParameters, new Condition<TypeParameterDescriptor>() {
            @Override
            public boolean value(TypeParameterDescriptor descriptor) {
                return descriptor.getContainingDeclaration() == TypeVariableResolver.this.owner;
            }
        }) : "Type parameters should be parameters of owner: " + owner + "; " + typeParameters;
    }

    @NotNull
    public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
        return getTypeVariable(name, typeParameters, owner);
    }

    @NotNull
    private TypeParameterDescriptor getTypeVariable(
            @NotNull String name,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull DeclarationDescriptor owner
    ) {
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            if (typeParameter.getName().asString().equals(name)) {
                return typeParameter;
            }
        }

        DeclarationDescriptor container = owner.getContainingDeclaration();
        if (container instanceof ClassDescriptor) {
            return getTypeVariable(name, ((ClassDescriptor) container).getTypeConstructor().getParameters(), container);
        }

        throw new IllegalStateException("Type parameter not found by name '" + name + "' in " + context);
    }
}
