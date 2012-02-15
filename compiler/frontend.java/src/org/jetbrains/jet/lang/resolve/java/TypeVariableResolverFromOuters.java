/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

/**
 * @author Stepan Koltsov
 */
public class TypeVariableResolverFromOuters implements TypeVariableResolver {

    @NotNull
    private final DeclarationDescriptor outer;

    public TypeVariableResolverFromOuters(@NotNull DeclarationDescriptor outer) {
        this.outer = outer;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
        DeclarationDescriptor outer = this.outer;
        for (;;) {
            if (outer instanceof NamespaceDescriptor) {
                throw new IllegalStateException("unresolve type parameter: " + name);
            } else if (outer instanceof ClassDescriptor) {
                for (TypeParameterDescriptor typeParameter : ((ClassDescriptor) outer).getTypeConstructor().getParameters()) {
                    if (typeParameter.getName().equals(name)) {
                        return typeParameter;
                    }
                }
                outer = outer.getContainingDeclaration();
            } else {
                throw new IllegalStateException("unknown outer: " + outer.getClass().getName());
            }
        }
    }
}
