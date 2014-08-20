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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.types.TypeUtils.topologicallySortSuperclassesAndRecordAllInstances;
import static org.jetbrains.jet.lang.types.Variance.IN_VARIANCE;
import static org.jetbrains.jet.lang.types.Variance.OUT_VARIANCE;

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
        assert !types.isEmpty();
        Collection<JetType> typeSet = new HashSet<JetType>(types);
        if (typeSet.size() == 1) return typeSet.iterator().next();

        // If any of the types is nullable, the result must be nullable
        // This also removed Nothing and Nothing? because they are subtypes of everything else
        boolean nullable = false;
        for (Iterator<JetType> iterator = typeSet.iterator(); iterator.hasNext();) {
            JetType type = iterator.next();
            assert type != null;
            if (KotlinBuiltIns.getInstance().isNothingOrNullableNothing(type)) {
                iterator.remove();
            }
            if (type.isError()) {
                return ErrorUtils.createErrorType("Supertype of error type " + type);
            }
            nullable |= type.isNullable();
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
        JetType result = computeSupertypeProjections(entry.getKey(), entry.getValue());
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
    private static JetType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<JetType> types) {
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
            newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections));
        }

        boolean nullable = false;
        for (JetType type : types) {
            nullable |= type.isNullable();
        }

        // TODO : attributes?
        JetScope newScope = JetScope.EMPTY;
        DeclarationDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();
        if (declarationDescriptor instanceof ClassDescriptor) {
            newScope = ((ClassDescriptor) declarationDescriptor).getMemberScope(newProjections);
        }
        return new JetTypeImpl(Annotations.EMPTY, constructor, nullable, newProjections, newScope);
    }

    @NotNull
    private static TypeProjection computeSupertypeProjection(@NotNull TypeParameterDescriptor parameterDescriptor, @NotNull Set<TypeProjection> typeProjections) {
        if (typeProjections.size() == 1) {
            return typeProjections.iterator().next();
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
            if (projectionKind.allowsInPosition()) {
                if (ins != null) {
                    ins.add(projection.getType());
                }
            }
            else {
                ins = null;
            }

            if (projectionKind.allowsOutPosition()) {
                if (outs != null) {
                    outs.add(projection.getType());
                }
            }
            else {
                outs = null;
            }
        }

        if (ins != null) {
            JetType intersection = TypeUtils.intersect(JetTypeChecker.DEFAULT, ins);
            if (intersection == null) {
                if (outs != null) {
                    return new TypeProjectionImpl(OUT_VARIANCE, commonSupertype(outs));
                }
                return new TypeProjectionImpl(OUT_VARIANCE, commonSupertype(parameterDescriptor.getUpperBounds()));
            }
            Variance projectionKind = variance == IN_VARIANCE ? Variance.INVARIANT : IN_VARIANCE;
            return new TypeProjectionImpl(projectionKind, intersection);
        }
        else if (outs != null) {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjectionImpl(projectionKind, commonSupertype(outs));
        }
        else {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjectionImpl(projectionKind, commonSupertype(parameterDescriptor.getUpperBounds()));
        }
    }

    private static void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (JetType type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }
}
