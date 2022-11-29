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

package org.jetbrains.kotlin.types;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.utils.DFS;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoundsSubstitutor {
    private BoundsSubstitutor() {
    }

    @NotNull
    public static <D extends CallableDescriptor> D substituteBounds(@NotNull D functionDescriptor) {
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        if (typeParameters.isEmpty()) return functionDescriptor;

        // TODO: this does not handle any recursion in the bounds
        @SuppressWarnings("unchecked")
        D substitutedFunction = (D) functionDescriptor.substitute(createUpperBoundsSubstitutor(typeParameters));
        assert substitutedFunction != null : "Substituting upper bounds should always be legal";

        return substitutedFunction;
    }

    @NotNull
    public static <D extends CallableDescriptor> TypeSubstitutor createUpperBoundsSubstitutor(@NotNull D callableDescriptor) {
        return createUpperBoundsSubstitutor(callableDescriptor.getTypeParameters());
    }

    @NotNull
    private static TypeSubstitutor createUpperBoundsSubstitutor(@NotNull List<TypeParameterDescriptor> typeParameters) {
        Map<TypeConstructor, TypeProjection> mutableSubstitution = new HashMap<>();
        TypeSubstitutor substitutor = TypeSubstitutor.create(mutableSubstitution);

        // todo assert: no loops
        for (TypeParameterDescriptor descriptor : topologicallySortTypeParameters(typeParameters)) {
            KotlinType upperBoundsAsType = TypeIntersector.getUpperBoundsAsType(descriptor);
            KotlinType substitutedUpperBoundsAsType = substitutor.substitute(upperBoundsAsType, Variance.INVARIANT);
            mutableSubstitution.put(descriptor.getTypeConstructor(), new TypeProjectionImpl(substitutedUpperBoundsAsType));
        }

        return substitutor;
    }

    @NotNull
    private static List<TypeParameterDescriptor> topologicallySortTypeParameters(@NotNull List<TypeParameterDescriptor> typeParameters) {
        // In the end, we want every parameter to have no references to those after it in the list
        // This gives us the reversed order: the one that refers to everybody else comes first
        List<TypeParameterDescriptor> topOrder = DFS.topologicalOrder(
                typeParameters, current -> getTypeParametersFromUpperBounds(current, typeParameters)
        );

        assert topOrder.size() == typeParameters.size() : "All type parameters must be visited, but only " + topOrder + " were";

        // Now, the one that refers to everybody else stands in the last position
        Collections.reverse(topOrder);
        return topOrder;
    }

    @NotNull
    private static List<TypeParameterDescriptor> getTypeParametersFromUpperBounds(
            @NotNull TypeParameterDescriptor current,
            @NotNull List<TypeParameterDescriptor> typeParameters
    ) {
        return DFS.dfs(
                current.getUpperBounds(),
                typeParameter -> CollectionsKt.map(typeParameter.getArguments(), TypeProjection::getType),
                new DFS.NodeHandlerWithListResult<KotlinType, TypeParameterDescriptor>() {
                    @Override
                    public boolean beforeChildren(KotlinType current) {
                        ClassifierDescriptor declarationDescriptor = current.getConstructor().getDeclarationDescriptor();
                        // typeParameters in a list, but it contains very few elements, so it's fine to call contains() on it
                        //noinspection SuspiciousMethodCalls
                        if (typeParameters.contains(declarationDescriptor)) {
                            result.add((TypeParameterDescriptor) declarationDescriptor);
                        }

                        return true;
                    }
                }
        );
    }
}
