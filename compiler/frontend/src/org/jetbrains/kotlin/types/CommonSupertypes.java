/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;
import org.jetbrains.kotlin.utils.DFS;

import java.util.*;

import static org.jetbrains.kotlin.types.Variance.IN_VARIANCE;
import static org.jetbrains.kotlin.types.Variance.OUT_VARIANCE;

public class CommonSupertypes {
    @Nullable
    public static KotlinType commonSupertypeForNonDenotableTypes(@NotNull Collection<KotlinType> types) {
        if (types.isEmpty()) return null;
        if (types.size() == 1) {
            KotlinType type = types.iterator().next();
            if (type.getConstructor() instanceof IntersectionTypeConstructor) {
                return commonSupertypeForNonDenotableTypes(type.getConstructor().getSupertypes());
            }
        }
        return commonSupertype(types);
    }

    @NotNull
    public static KotlinType commonSupertype(@NotNull Collection<KotlinType> types) {
        // Recursion should not be significantly deeper than the deepest type in question
        // It can be slightly deeper, though: e.g. when initial types are simple, but their supertypes are complex
        return findCommonSupertype(types, 0, maxDepth(types) + 3);
    }

    private static int maxDepth(@NotNull Collection<KotlinType> types) {
        int max = 0;
        for (KotlinType type : types) {
            int depth = depth(type);
            if (max < depth) {
                max = depth;
            }
        }
        return max;
    }

    private static int depth(@NotNull final KotlinType type) {
        return 1 + maxDepth(CollectionsKt.map(type.getArguments(), new Function1<TypeProjection, KotlinType>() {
            @Override
            public KotlinType invoke(TypeProjection projection) {
                if (projection.isStarProjection()) {
                    // any type is good enough for depth here
                    return type.getConstructor().getBuiltIns().getAnyType();
                }
                return projection.getType();
            }
        }));
    }

    @NotNull
    private static KotlinType findCommonSupertype(@NotNull Collection<KotlinType> types, int recursionDepth, int maxDepth) {
        assert recursionDepth <= maxDepth : "Recursion depth exceeded: " + recursionDepth + " > " + maxDepth + " for types " + types;
        boolean hasFlexible = false;
        List<KotlinType> upper = new ArrayList<KotlinType>(types.size());
        List<KotlinType> lower = new ArrayList<KotlinType>(types.size());
        Set<FlexibleTypeFactory> factories = new LinkedHashSet<FlexibleTypeFactory>();
        for (KotlinType type : types) {
            if (FlexibleTypesKt.isFlexible(type)) {
                if (DynamicTypesKt.isDynamic(type)) {
                    return type;
                }
                hasFlexible = true;
                Flexibility flexibility = FlexibleTypesKt.flexibility(type);
                upper.add(flexibility.getUpperBound());
                lower.add(flexibility.getLowerBound());
                factories.add(flexibility.getFactory());
            }
            else {
                upper.add(type);
                lower.add(type);
            }
        }

        if (!hasFlexible) return commonSuperTypeForInflexible(types, recursionDepth, maxDepth);
        return CollectionsKt.single(factories).create( // mixing different factories is not supported
                commonSuperTypeForInflexible(lower, recursionDepth, maxDepth),
                commonSuperTypeForInflexible(upper, recursionDepth, maxDepth)
        );
    }

    @NotNull
    private static KotlinType commonSuperTypeForInflexible(@NotNull Collection<KotlinType> types, int recursionDepth, int maxDepth) {
        assert !types.isEmpty();
        Collection<KotlinType> typeSet = new HashSet<KotlinType>(types);

        KotlinType bestFit = FlexibleTypesKt.singleBestRepresentative(typeSet);
        if (bestFit != null) return bestFit;

        // If any of the types is nullable, the result must be nullable
        // This also removed Nothing and Nothing? because they are subtypes of everything else
        boolean nullable = false;
        for (Iterator<KotlinType> iterator = typeSet.iterator(); iterator.hasNext();) {
            KotlinType type = iterator.next();
            assert type != null;
            assert !FlexibleTypesKt.isFlexible(type) : "Flexible type " + type + " passed to commonSuperTypeForInflexible";
            if (KotlinBuiltIns.isNothingOrNullableNothing(type)) {
                iterator.remove();
            }
            if (type.isError()) {
                return ErrorUtils.createErrorType("Supertype of error type " + type);
            }
            nullable |= type.isMarkedNullable();
        }

        // Everything deleted => it's Nothing or Nothing?
        if (typeSet.isEmpty()) {
            // TODO : attributes
            KotlinBuiltIns builtIns = types.iterator().next().getConstructor().getBuiltIns();
            return nullable ? builtIns.getNullableNothingType() : builtIns.getNothingType();
        }

        if (typeSet.size() == 1) {
            return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
        }

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map<TypeConstructor, Set<KotlinType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
        while (commonSupertypes.size() > 1) {
            Set<KotlinType> merge = new HashSet<KotlinType>();
            for (Set<KotlinType> supertypes : commonSupertypes.values()) {
                merge.addAll(supertypes);
            }
            commonSupertypes = computeCommonRawSupertypes(merge);
        }
        assert !commonSupertypes.isEmpty() : commonSupertypes + " <- " + types;

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map.Entry<TypeConstructor, Set<KotlinType>> entry = commonSupertypes.entrySet().iterator().next();

        // Reconstructing type arguments if possible
        KotlinType result = computeSupertypeProjections(entry.getKey(), entry.getValue(), recursionDepth, maxDepth);
        return TypeUtils.makeNullableIfNeeded(result, nullable);
    }

    // Raw supertypes are superclasses w/o type arguments
    // @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
    @NotNull
    private static Map<TypeConstructor, Set<KotlinType>> computeCommonRawSupertypes(@NotNull Collection<KotlinType> types) {
        assert !types.isEmpty();

        Map<TypeConstructor, Set<KotlinType>> constructorToAllInstances = new HashMap<TypeConstructor, Set<KotlinType>>();
        Set<TypeConstructor> commonSuperclasses = null;

        List<TypeConstructor> order = null;
        for (KotlinType type : types) {
            Set<TypeConstructor> visited = new HashSet<TypeConstructor>();
            order = topologicallySortSuperclassesAndRecordAllInstances(type, constructorToAllInstances, visited);

            if (commonSuperclasses == null) {
                commonSuperclasses = visited;
            }
            else {
                commonSuperclasses.retainAll(visited);
            }
        }
        assert order != null;

        Set<TypeConstructor> notSource = new HashSet<TypeConstructor>();
        Map<TypeConstructor, Set<KotlinType>> result = new HashMap<TypeConstructor, Set<KotlinType>>();
        for (TypeConstructor superConstructor : order) {
            if (!commonSuperclasses.contains(superConstructor)) {
                continue;
            }

            if (!notSource.contains(superConstructor)) {
                result.put(superConstructor, constructorToAllInstances.get(superConstructor));
                markAll(superConstructor, notSource);
            }
        }

        return result;
    }

    // constructor - type constructor of a supertype to be instantiated
    // types - instantiations of constructor occurring as supertypes of classes we are trying to intersect
    @NotNull
    private static KotlinType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<KotlinType> types, int recursionDepth, int maxDepth) {
        // we assume that all the given types are applications of the same type constructor

        assert !types.isEmpty();

        if (types.size() == 1) {
            return types.iterator().next();
        }

        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        List<TypeProjection> newProjections = new ArrayList<TypeProjection>(parameters.size());
        for (TypeParameterDescriptor parameterDescriptor : parameters) {
            Set<TypeProjection> typeProjections = new HashSet<TypeProjection>();
            for (KotlinType type : types) {
                typeProjections.add(type.getArguments().get(parameterDescriptor.getIndex()));
            }
            newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections, recursionDepth, maxDepth));
        }

        boolean nullable = false;
        for (KotlinType type : types) {
            nullable |= type.isMarkedNullable();
        }

        ClassifierDescriptor classifier = constructor.getDeclarationDescriptor();
        MemberScope newScope;
        if (classifier instanceof ClassDescriptor) {
            newScope = ((ClassDescriptor) classifier).getMemberScope(newProjections);
        }
        else if (classifier instanceof TypeParameterDescriptor) {
            newScope = classifier.getDefaultType().getMemberScope();
        }
        else {
            newScope = ErrorUtils.createErrorScope("A scope for common supertype which is not a normal classifier", true);
        }
        return KotlinTypeFactory.simpleType(Annotations.Companion.getEMPTY(), constructor, newProjections, nullable, newScope);
    }

    @NotNull
    private static TypeProjection computeSupertypeProjection(
            @NotNull TypeParameterDescriptor parameterDescriptor,
            @NotNull Set<TypeProjection> typeProjections,
            int recursionDepth, int maxDepth
    ) {
        TypeProjection singleBestProjection = FlexibleTypesKt.singleBestRepresentative(typeProjections);
        if (singleBestProjection != null) {
            return singleBestProjection;
        }

        if (recursionDepth >= maxDepth) {
            // If recursion is too deep, we cut it by taking <out Any?> as an ultimate supertype argument
            // Example: class A : Base<A>; class B : Base<B>, commonSuperType(A, B) = Base<*>
            return TypeUtils.makeStarProjection(parameterDescriptor);
        }

        Set<KotlinType> ins = new HashSet<KotlinType>();
        Set<KotlinType> outs = new HashSet<KotlinType>();

        Variance variance = parameterDescriptor.getVariance();
        switch (variance) {
            case INVARIANT:
                // Nothing
                break;
            case IN_VARIANCE:
                outs = null;
                break;
            case OUT_VARIANCE:
                ins = null;
                break;
        }

        for (TypeProjection projection : typeProjections) {
            Variance projectionKind = projection.getProjectionKind();
            if (projectionKind.getAllowsInPosition()) {
                if (ins != null) {
                    ins.add(projection.getType());
                }
            }
            else {
                ins = null;
            }

            if (projectionKind.getAllowsOutPosition()) {
                if (outs != null) {
                    outs.add(projection.getType());
                }
            }
            else {
                outs = null;
            }
        }

        if (outs != null) {
            assert !outs.isEmpty() : "Out projections is empty for parameter " + parameterDescriptor + ", type projections " + typeProjections;
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            KotlinType superType = findCommonSupertype(outs, recursionDepth + 1, maxDepth);
            for (KotlinType upperBound: parameterDescriptor.getUpperBounds()) {
                if (!TypeUtilsKt.isSubtypeOf(superType, upperBound)) {
                    return new StarProjectionImpl(parameterDescriptor);
                }
            }
            return new TypeProjectionImpl(projectionKind, superType);
        }
        if (ins != null) {
            assert !ins.isEmpty() : "In projections is empty for parameter " + parameterDescriptor + ", type projections " + typeProjections;
            KotlinType intersection = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, ins);
            if (intersection == null) {
                return TypeUtils.makeStarProjection(parameterDescriptor);
            }
            Variance projectionKind = variance == IN_VARIANCE ? Variance.INVARIANT : IN_VARIANCE;
            return new TypeProjectionImpl(projectionKind, intersection);
        }
        else {
            return TypeUtils.makeStarProjection(parameterDescriptor);
        }
    }

    private static void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (KotlinType type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }

    @NotNull
    public static List<TypeConstructor> topologicallySortSuperclassesAndRecordAllInstances(
            @NotNull KotlinType type,
            @NotNull final Map<TypeConstructor, Set<KotlinType>> constructorToAllInstances,
            @NotNull final Set<TypeConstructor> visited
    ) {
        return DFS.dfs(
                Collections.singletonList(type),
                new DFS.Neighbors<KotlinType>() {
                    @NotNull
                    @Override
                    public Iterable<KotlinType> getNeighbors(KotlinType current) {
                        TypeSubstitutor substitutor = TypeSubstitutor.create(current);
                        Collection<KotlinType> supertypes = current.getConstructor().getSupertypes();
                        List<KotlinType> result = new ArrayList<KotlinType>(supertypes.size());
                        for (KotlinType supertype : supertypes) {
                            if (visited.contains(supertype.getConstructor())) {
                                continue;
                            }
                            result.add(substitutor.safeSubstitute(supertype, Variance.INVARIANT));
                        }
                        return result;
                    }
                },
                new DFS.Visited<KotlinType>() {
                    @Override
                    public boolean checkAndMarkVisited(KotlinType current) {
                        return visited.add(current.getConstructor());
                    }
                },
                new DFS.NodeHandlerWithListResult<KotlinType, TypeConstructor>() {
                    @Override
                    public boolean beforeChildren(KotlinType current) {
                        TypeConstructor constructor = current.getConstructor();

                        Set<KotlinType> instances = constructorToAllInstances.get(constructor);
                        if (instances == null) {
                            instances = new HashSet<KotlinType>();
                            constructorToAllInstances.put(constructor, instances);
                        }
                        instances.add(current);

                        return true;
                    }

                    @Override
                    public void afterChildren(KotlinType current) {
                        result.addFirst(current.getConstructor());
                    }
                }
        );
    }
}
