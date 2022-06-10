/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.types.checker.IntersectionTypeKt;
import org.jetbrains.kotlin.types.*;
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
    private static TypeSubstitutor createUpperBoundsSubstitutor(@NotNull List<TypeParameterDescriptor> typeParameters) {
        Map<TypeConstructor, TypeProjection> mutableSubstitution = new HashMap<>();
        TypeSubstitutor substitutor = TypeSubstitutor.create(mutableSubstitution);

        // todo assert: no loops
        for (TypeParameterDescriptor descriptor : topologicallySortTypeParameters(typeParameters)) {
            KotlinType upperBoundsAsType = IntersectionTypeKt.intersectWrappedTypes(descriptor.getUpperBounds());
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
