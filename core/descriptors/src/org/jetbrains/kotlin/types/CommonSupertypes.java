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

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.types.TypeUtils.topologicallySortSuperclassesAndRecordAllInstances;
import static org.jetbrains.kotlin.types.Variance.IN_VARIANCE;
import static org.jetbrains.kotlin.types.Variance.OUT_VARIANCE;

public class CommonSupertypes {
    @Nullable
    public static JetType commonSupertypeForNonDenotableTypes(@NotNull Collection<JetType> types) {
        if (types.isEmpty()) return null;
        if (types.size() == 1) {
            JetType type = types.iterator().next();
            if (type.getConstructor() instanceof IntersectionTypeConstructor) {
                return commonSupertypeForNonDenotableTypes(type.getConstructor().getSupertypes());
            }
        }
        return commonSupertype(types);
    }

    @NotNull
    public static JetType commonSupertype(@NotNull Collection<JetType> types) {
        // Recursion should not be significantly deeper than the deepest type in question
        // It can be slightly deeper, though: e.g. when initial types are simple, but their supertypes are complex
        return findCommonSupertype(types, 0, maxDepth(types) + 3);
    }

    private static int maxDepth(@NotNull Collection<JetType> types) {
        int max = 0;
        for (JetType type : types) {
            int depth = depth(type);
            if (max < depth) {
                max = depth;
            }
        }
        return max;
    }

    private static int depth(@NotNull JetType type) {
        return 1 + maxDepth(KotlinPackage.map(type.getArguments(), new Function1<TypeProjection, JetType>() {
            @Override
            public JetType invoke(TypeProjection projection) {
                if (projection.isStarProjection()) {
                    // any type is good enough for depth here
                    return KotlinBuiltIns.getInstance().getAnyType();
                }
                return projection.getType();
            }
        }));
    }

    @NotNull
    private static JetType findCommonSupertype(@NotNull Collection<JetType> types, int recursionDepth, int maxDepth) {
        assert recursionDepth <= maxDepth : "Recursion depth exceeded: " + recursionDepth + " > " + maxDepth + " for types " + types;
        boolean hasFlexible = false;
        List<JetType> upper = new ArrayList<JetType>(types.size());
        List<JetType> lower = new ArrayList<JetType>(types.size());
        Set<FlexibleTypeCapabilities> capabilities = new LinkedHashSet<FlexibleTypeCapabilities>();
        for (JetType type : types) {
            if (TypesPackage.isFlexible(type)) {
                hasFlexible = true;
                Flexibility flexibility = TypesPackage.flexibility(type);
                upper.add(flexibility.getUpperBound());
                lower.add(flexibility.getLowerBound());
                capabilities.add(flexibility.getExtraCapabilities());
            }
            else {
                upper.add(type);
                lower.add(type);
            }
        }

        if (!hasFlexible) return commonSuperTypeForInflexible(types, recursionDepth, maxDepth);
        return DelegatingFlexibleType.create(
                commonSuperTypeForInflexible(lower, recursionDepth, maxDepth),
                commonSuperTypeForInflexible(upper, recursionDepth, maxDepth),
                KotlinPackage.single(capabilities) // mixing different capabilities is not supported
        );
    }

    @NotNull
    private static JetType commonSuperTypeForInflexible(@NotNull Collection<JetType> types, int recursionDepth, int maxDepth) {
        assert !types.isEmpty();
        Collection<JetType> typeSet = new HashSet<JetType>(types);

        JetType bestFit = TypesPackage.singleBestRepresentative(typeSet);
        if (bestFit != null) return bestFit;

        // If any of the types is nullable, the result must be nullable
        // This also removed Nothing and Nothing? because they are subtypes of everything else
        boolean nullable = false;
        for (Iterator<JetType> iterator = typeSet.iterator(); iterator.hasNext();) {
            JetType type = iterator.next();
            assert type != null;
            assert !TypesPackage.isFlexible(type) : "Flexible type " + type + " passed to commonSuperTypeForInflexible";
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
            return nullable ? KotlinBuiltIns.getInstance().getNullableNothingType() : KotlinBuiltIns.getInstance().getNothingType();
        }

        if (typeSet.size() == 1) {
            return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
        }

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map<TypeConstructor, Set<JetType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
        while (commonSupertypes.size() > 1) {
            Set<JetType> merge = new HashSet<JetType>();
            for (Set<JetType> supertypes : commonSupertypes.values()) {
                merge.addAll(supertypes);
            }
            commonSupertypes = computeCommonRawSupertypes(merge);
        }
        assert !commonSupertypes.isEmpty() : commonSupertypes + " <- " + types;

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map.Entry<TypeConstructor, Set<JetType>> entry = commonSupertypes.entrySet().iterator().next();

        // Reconstructing type arguments if possible
        JetType result = computeSupertypeProjections(entry.getKey(), entry.getValue(), recursionDepth, maxDepth);
        return TypeUtils.makeNullableIfNeeded(result, nullable);
    }

    // Raw supertypes are superclasses w/o type arguments
    // @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
    @NotNull
    private static Map<TypeConstructor, Set<JetType>> computeCommonRawSupertypes(@NotNull Collection<JetType> types) {
        assert !types.isEmpty();

        Map<TypeConstructor, Set<JetType>> constructorToAllInstances = new HashMap<TypeConstructor, Set<JetType>>();
        Set<TypeConstructor> commonSuperclasses = null;

        List<TypeConstructor> order = null;
        for (JetType type : types) {
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
        Map<TypeConstructor, Set<JetType>> result = new HashMap<TypeConstructor, Set<JetType>>();
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
    private static JetType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<JetType> types, int recursionDepth, int maxDepth) {
        // we assume that all the given types are applications of the same type constructor

        assert !types.isEmpty();

        if (types.size() == 1) {
            return types.iterator().next();
        }

        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        List<TypeProjection> newProjections = new ArrayList<TypeProjection>();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameterDescriptor = parameters.get(i);
            Set<TypeProjection> typeProjections = new HashSet<TypeProjection>();
            for (JetType type : types) {
                typeProjections.add(type.getArguments().get(i));
            }
            newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections, recursionDepth, maxDepth));
        }

        boolean nullable = false;
        for (JetType type : types) {
            nullable |= type.isMarkedNullable();
        }

        ClassifierDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();
        JetScope newScope;
        if (declarationDescriptor instanceof ClassDescriptor) {
            newScope = ((ClassDescriptor) declarationDescriptor).getMemberScope(newProjections);
        }
        else if (declarationDescriptor instanceof TypeParameterDescriptor) {
            newScope = ((TypeParameterDescriptor) declarationDescriptor).getUpperBoundsAsType().getMemberScope();
        }
        else {
            newScope = ErrorUtils.createErrorScope("A scope for common supertype which is not a normal classifier", true);
        }
        return new JetTypeImpl(Annotations.EMPTY, constructor, nullable, newProjections, newScope);
    }

    @NotNull
    private static TypeProjection computeSupertypeProjection(
            @NotNull TypeParameterDescriptor parameterDescriptor,
            @NotNull Set<TypeProjection> typeProjections,
            int recursionDepth, int maxDepth
    ) {
        TypeProjection singleBestProjection = TypesPackage.singleBestRepresentative(typeProjections);
        if (singleBestProjection != null) {
            return singleBestProjection;
        }

        if (recursionDepth >= maxDepth) {
            // If recursion is too deep, we cut it by taking <out Any?> as an ultimate supertype argument
            // Example: class A : Base<A>; class B : Base<B>, commonSuperType(A, B) = Base<out Any?>
            return new TypeProjectionImpl(OUT_VARIANCE, KotlinBuiltIns.getInstance().getNullableAnyType());
        }

        Set<JetType> ins = new HashSet<JetType>();
        Set<JetType> outs = new HashSet<JetType>();

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
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjectionImpl(projectionKind, findCommonSupertype(outs, recursionDepth + 1, maxDepth));
        }
        if (ins != null) {
            JetType intersection = TypeUtils.intersect(JetTypeChecker.DEFAULT, ins);
            if (intersection == null) {
                return new TypeProjectionImpl(OUT_VARIANCE, findCommonSupertype(parameterDescriptor.getUpperBounds(), recursionDepth + 1, maxDepth));
            }
            Variance projectionKind = variance == IN_VARIANCE ? Variance.INVARIANT : IN_VARIANCE;
            return new TypeProjectionImpl(projectionKind, intersection);
        }
        else {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjectionImpl(projectionKind, findCommonSupertype(parameterDescriptor.getUpperBounds(), recursionDepth + 1, maxDepth));
        }
    }

    private static void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (JetType type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }
}
