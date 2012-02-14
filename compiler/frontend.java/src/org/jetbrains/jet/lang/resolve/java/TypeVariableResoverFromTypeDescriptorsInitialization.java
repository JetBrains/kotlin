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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class TypeVariableResoverFromTypeDescriptorsInitialization extends TypeVariableByPsiResolverImpl implements TypeVariableResolver {

    @NotNull
    private final List<JavaDescriptorResolver.TypeParameterDescriptorInitialization> typeParameters;
    @Nullable
    private final TypeVariableResolver parent;

    public TypeVariableResoverFromTypeDescriptorsInitialization(@NotNull List<JavaDescriptorResolver.TypeParameterDescriptorInitialization> typeParameters, @Nullable TypeVariableResolver parent) {
        super(typeParameters, parent);
        this.typeParameters = typeParameters;
        this.parent = parent;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
        for (JavaDescriptorResolver.TypeParameterDescriptorInitialization typeParameter : typeParameters) {
            if (typeParameter.descriptor.getName().equals(name)) {
                return typeParameter.descriptor;
            }
        }
        if (parent != null) {
            return parent.getTypeVariable(name);
        }
        throw new RuntimeException("type parameter not found by name " + name); // TODO report properly
    }
}
