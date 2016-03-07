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

package org.jetbrains.kotlin.resolve.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeProjectionImpl;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.*;

public class JavaResolverUtils {
    private JavaResolverUtils() {
    }

    public static Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> recreateTypeParametersAndReturnMapping(
            @NotNull List<TypeParameterDescriptor> originalParameters,
            @Nullable DeclarationDescriptor newOwner
    ) {
        // LinkedHashMap to save the order of type parameters
        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> result =
                new LinkedHashMap<TypeParameterDescriptor, TypeParameterDescriptorImpl>();
        for (TypeParameterDescriptor typeParameter : originalParameters) {
            result.put(typeParameter,
                       TypeParameterDescriptorImpl.createForFurtherModification(
                               newOwner == null ? typeParameter.getContainingDeclaration() : newOwner,
                               typeParameter.getAnnotations(),
                               typeParameter.isReified(),
                               typeParameter.getVariance(),
                               typeParameter.getName(),
                               typeParameter.getIndex(),
                               SourceElement.NO_SOURCE
                       )
            );
        }
        return result;
    }

    @NotNull
    public static TypeSubstitutor createSubstitutorForTypeParameters(
            @NotNull Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters
    ) {
        Map<TypeConstructor, TypeProjection> typeSubstitutionContext = new HashMap<TypeConstructor, TypeProjection>();
        for (Map.Entry<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameter : originalToAltTypeParameters.entrySet()) {
            typeSubstitutionContext.put(originalToAltTypeParameter.getKey().getTypeConstructor(),
                                        new TypeProjectionImpl(originalToAltTypeParameter.getValue().getDefaultType()));
        }
        // TODO: Use IndexedParametersSubstitution here instead of map creation
        return TypeSubstitutor.create(typeSubstitutionContext);
    }
}
