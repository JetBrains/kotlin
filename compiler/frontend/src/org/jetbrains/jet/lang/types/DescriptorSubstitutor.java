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

package org.jetbrains.jet.lang.types;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.utils.DFS;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DescriptorSubstitutor {

    private static final Function<TypeProjection,JetType> PROJECTIONS_TO_TYPES = new Function<TypeProjection, JetType>() {
        @Override
        public JetType apply(TypeProjection projection) {
            return projection.getType();
        }
    };

    @NotNull
    public static TypeSubstitutor substituteTypeParameters(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull final TypeSubstitutor originalSubstitutor,
            @NotNull DeclarationDescriptor newContainingDeclaration,
            @NotNull List<TypeParameterDescriptor> result) {
        final Map<TypeConstructor, TypeProjection> mutableSubstitution = Maps.newHashMap();
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
        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> substitutedMap = Maps.newHashMap();
        for (TypeParameterDescriptor descriptor : typeParameters) {
            TypeParameterDescriptorImpl substituted = TypeParameterDescriptorImpl.createForFurtherModification(
                    newContainingDeclaration,
                    descriptor.getAnnotations(),
                    descriptor.isReified(),
                    descriptor.getVariance(),
                    descriptor.getName(),
                    descriptor.getIndex());
            substituted.setInitialized();

            mutableSubstitution.put(descriptor.getTypeConstructor(), new TypeProjection(substituted.getDefaultType()));

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



    @NotNull
    public static TypeSubstitutor createUpperBoundsSubstitutor(
            @NotNull final List<TypeParameterDescriptor> typeParameters
    ) {
        Map<TypeConstructor, TypeProjection> mutableSubstitution = Maps.newHashMap();
        TypeSubstitutor substitutor = TypeSubstitutor.create(mutableSubstitution);

        // todo assert: no loops
        for (TypeParameterDescriptor descriptor : topologicallySortTypeParameters(typeParameters)) {
            JetType upperBoundsAsType = descriptor.getUpperBoundsAsType();
            JetType substitutedUpperBoundsAsType = substitutor.substitute(upperBoundsAsType, Variance.INVARIANT);
            mutableSubstitution.put(descriptor.getTypeConstructor(), new TypeProjection(substitutedUpperBoundsAsType));
        }

        return substitutor;
    }

    private static List<TypeParameterDescriptor> topologicallySortTypeParameters(final List<TypeParameterDescriptor> typeParameters) {
        // In the end, we want every parameter to have no references to those after it in the list
        // This gives us the reversed order: the one that refers to everybody else comes first
        List<TypeParameterDescriptor> topOrder = DFS.topologicalOrder(
                typeParameters,
                new DFS.Neighbors<TypeParameterDescriptor>() {
                    @NotNull
                    @Override
                    public Iterable<TypeParameterDescriptor> getNeighbors(TypeParameterDescriptor current) {
                        return getTypeParametersFromUpperBounds(current, typeParameters);
                    }
                });

        assert topOrder.size() == typeParameters.size() : "All type parameters must be visited, but only " + topOrder + " were";

        // Now, the one that refers to everybody else stands in the last position
        Collections.reverse(topOrder);
        return topOrder;
    }

    private static List<TypeParameterDescriptor> getTypeParametersFromUpperBounds(
            TypeParameterDescriptor current,
            final List<TypeParameterDescriptor> typeParameters
    ) {
        return DFS.dfs(
                current.getUpperBounds(),
                new DFS.Neighbors<JetType>() {
                    @NotNull
                    @Override
                    public Iterable<JetType> getNeighbors(JetType current) {
                        return Collections2.transform(current.getArguments(), PROJECTIONS_TO_TYPES);
                    }
                },
                new DFS.NodeHandlerWithListResult<JetType, TypeParameterDescriptor>() {
                    @Override
                    public void beforeChildren(JetType current) {
                        ClassifierDescriptor declarationDescriptor = current.getConstructor().getDeclarationDescriptor();
                        // typeParameters in a list, but it contains very few elements, so it's fine to call contains() on it
                        //noinspection SuspiciousMethodCalls
                        if (typeParameters.contains(declarationDescriptor)) {
                            result.add((TypeParameterDescriptor) declarationDescriptor);
                        }
                    }
                }
        );
    }
}
