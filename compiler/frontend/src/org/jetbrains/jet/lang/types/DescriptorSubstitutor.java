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

package org.jetbrains.jet.lang.types;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class DescriptorSubstitutor {

    @NotNull
    public static TypeSubstitutor substituteTypeParameters(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull final TypeSubstitutor originalSubstitutor,
            @NotNull DeclarationDescriptor newContainingDeclaration,
            @NotNull List<TypeParameterDescriptor> result) {
        final Map<TypeConstructor, TypeProjection> mutableSubstitution = Maps.newHashMap();
        TypeSubstitutor substitutor = TypeSubstitutor.create(new TypeSubstitutor.TypeSubstitution() {

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

        for (TypeParameterDescriptor descriptor : typeParameters) {
            TypeParameterDescriptor substituted = TypeParameterDescriptor.createForFurtherModification(
                    newContainingDeclaration,
                    descriptor.getAnnotations(),
                    descriptor.isReified(),
                    descriptor.getVariance(),
                    descriptor.getName(),
                    descriptor.getIndex());
            substituted.setInitialized();

            mutableSubstitution.put(descriptor.getTypeConstructor(), new TypeProjection(substituted.getDefaultType()));

            for (JetType upperBound : descriptor.getUpperBounds()) {
                substituted.getUpperBounds().add(substitutor.substitute(upperBound, Variance.INVARIANT));
            }

            result.add(substituted);
        }

        return substitutor;
    }

}
