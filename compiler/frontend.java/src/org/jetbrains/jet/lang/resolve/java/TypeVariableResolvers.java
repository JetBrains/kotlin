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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;

import java.util.ArrayList;
import java.util.List;

public class TypeVariableResolvers {

    @NotNull
    public static List<TypeParameterDescriptor> getTypeParameterDescriptors(@NotNull ClassOrPackageDescriptor clazz) {
        if (clazz instanceof ClassDescriptor) {
            return ((ClassDescriptor) clazz).getTypeConstructor().getParameters();
        }
        else {
            return new ArrayList<TypeParameterDescriptor>(0);
        }
    }

    @NotNull
    public static TypeVariableResolver classTypeVariableResolver(@NotNull ClassOrPackageDescriptor clazz, @NotNull String context) {
        return typeVariableResolverFromTypeParameters(getTypeParameterDescriptors(clazz), clazz, context);
    }

    @NotNull
    public static TypeVariableResolver typeVariableResolverFromTypeParameters(
            @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull DeclarationDescriptor owner, @NotNull String context) {

        return new TypeVariableResolverFromTypeDescriptors(typeParameters, owner, context);
    }
    
}
