/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DescriptorSubstitutor {
    private DescriptorSubstitutor() {
    }

    @NotNull
    public static TypeSubstitutor substituteTypeParameters(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull final TypeSubstitutor originalSubstitutor,
            @NotNull DeclarationDescriptor newContainingDeclaration,
            @NotNull List<TypeParameterDescriptor> result
    ) {
        final Map<TypeConstructor, TypeProjection> mutableSubstitution = new HashMap<TypeConstructor, TypeProjection>();
        TypeSubstitutor substitutor = TypeSubstitutor.create(new TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                if (originalSubstitutor.inRange(key)) {
                    return originalSubstitutor.getSubstitution().get(key);
                }
                return mutableSubstitution.get(key);
            }

            @Override
            public boolean isEmpty() {
                return originalSubstitutor.isEmpty() && mutableSubstitution.isEmpty();
            }

            @Override
            public String toString() {
                return "DescriptorSubstitutor.substituteTypeParameters(" + mutableSubstitution + " / " + originalSubstitutor.getSubstitution() + ")";
            }
        });

        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> substitutedMap =
                new HashMap<TypeParameterDescriptor, TypeParameterDescriptorImpl>();
        for (TypeParameterDescriptor descriptor : typeParameters) {
            TypeParameterDescriptorImpl substituted = TypeParameterDescriptorImpl.createForFurtherModification(
                    newContainingDeclaration,
                    descriptor.getAnnotations(),
                    descriptor.isReified(),
                    descriptor.getVariance(),
                    descriptor.getName(),
                    descriptor.getIndex(),
                    SourceElement.NO_SOURCE
            );
            substituted.setInitialized();

            mutableSubstitution.put(descriptor.getTypeConstructor(), new TypeProjectionImpl(substituted.getDefaultType()));

            substitutedMap.put(descriptor, substituted);
            result.add(substituted);
        }

        for (TypeParameterDescriptor descriptor : typeParameters) {
            TypeParameterDescriptorImpl substituted = substitutedMap.get(descriptor);
            for (JetType upperBound : descriptor.getUpperBounds()) {
                substituted.getUpperBounds().add(substitutor.substitute(upperBound, Variance.INVARIANT));
            }
        }

        return substitutor;
    }
}
