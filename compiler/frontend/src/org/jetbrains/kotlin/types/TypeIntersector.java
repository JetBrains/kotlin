/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.resolve.calls.inference.CallHandle;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.error.ErrorTypeKind;
import org.jetbrains.kotlin.types.error.ErrorUtils;

import java.util.*;

import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.SPECIAL;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class TypeIntersector {

    public static boolean isIntersectionEmpty(@NotNull KotlinType typeA, @NotNull KotlinType typeB) {
        return intersectTypes(new LinkedHashSet<>(Arrays.asList(typeA, typeB))) == null;
    }

    @Nullable
    public static KotlinType intersectTypes(@NotNull Collection<KotlinType> types) {
        assert !types.isEmpty() : "Attempting to intersect empty collection of types, this case should be dealt with on the call site.";

        if (types.size() == 1) {
            return types.iterator().next();
        }

        // Intersection of T1..Tn is an intersection of their non-null versions,
        //   made nullable is they all were nullable
        KotlinType nothingOrNullableNothing = null;
        boolean allNullable = true;
        List<KotlinType> nullabilityStripped = new ArrayList<>(types.size());
        for (KotlinType type : types) {
            if (KotlinTypeKt.isError(type)) continue;

            if (KotlinBuiltIns.isNothingOrNullableNothing(type)) {
                nothingOrNullableNothing = type;
            }
            allNullable &= type.isMarkedNullable();
            nullabilityStripped.add(TypeUtils.makeNotNullable(type));
        }

        if (nothingOrNullableNothing != null) {
            return TypeUtils.makeNullableAsSpecified(nothingOrNullableNothing, allNullable);
        }

        if (nullabilityStripped.isEmpty()) {
            // All types were errors
            return ErrorUtils.createErrorType(ErrorTypeKind.INTERSECTION_OF_ERROR_TYPES, types.toString());
        }

        KotlinTypeChecker typeChecker = KotlinTypeChecker.DEFAULT;
        // Now we remove types that have subtypes in the list
        List<KotlinType> resultingTypes = new ArrayList<>();
        outer:
        for (KotlinType type : nullabilityStripped) {
            if (!TypeUtils.canHaveSubtypes(typeChecker, type)) {
                boolean relativeToAll = true;
                for (KotlinType other : nullabilityStripped) {
                    // It makes sense to check for subtyping (other <: type), despite that
                    // type is not supposed to be open, for there're enums
                    boolean mayBeEqual = TypeUnifier.mayBeEqual(type, other);
                    boolean relative = typeChecker.isSubtypeOf(type, other) || typeChecker.isSubtypeOf(other, type);
                    if (!mayBeEqual && !relative) {
                        return null;
                    }
                    else if (!relative) {
                        // To build T & (final A), instead of returning just A as intersection
                        relativeToAll = false;
                        break;
                    }
                }
                if (relativeToAll) return TypeUtils.makeNullableAsSpecified(type, allNullable);
            }
            for (KotlinType other : nullabilityStripped) {
                if (!type.equals(other) && typeChecker.isSubtypeOf(other, type)) {
                    continue outer;
                }
            }

            // Don't add type if it is already present, to avoid trivial type intersections in result
            for (KotlinType other : resultingTypes) {
                if (typeChecker.equalTypes(other, type)) {
                    continue outer;
                }
            }
            resultingTypes.add(type);
        }

        if (resultingTypes.isEmpty()) {
            // If we ended up here, it means that all types from `nullabilityStripped` were excluded by the code above
            // most likely, this is because they are all semantically interchangeable (e.g. List<Foo>! and List<Foo>),
            // in that case, we can safely select the best representative out of that set and return it
            // TODO: maybe return the most specific among the types that are subtypes to all others in the `nullabilityStripped`?
            // TODO: e.g. among {Int, Int?, Int!}, return `Int` (now it returns `Int!`).
            KotlinType bestRepresentative = FlexibleTypesKt.singleBestRepresentative(nullabilityStripped);

            if (bestRepresentative == null) {
                bestRepresentative = UtilsKt.hackForTypeIntersector(nullabilityStripped);
            }

            if (bestRepresentative == null) {
                throw new AssertionError("Empty intersection for types " + types);
            }
            return TypeUtils.makeNullableAsSpecified(bestRepresentative, allNullable);
        }

        if (resultingTypes.size() == 1) {
            return TypeUtils.makeNullableAsSpecified(resultingTypes.get(0), allNullable);
        }

        return new IntersectionTypeConstructor(resultingTypes).createType();
    }

    /**
     * Note: this method was used in overload and override bindings to approximate type parameters with several bounds,
     * but as it turned out at some point, that logic was inconsistent with Java rules, so it was simplified.
     * Most of the other usages of this method are left untouched but probably should be investigated closely if they're still valid.
     */
    @NotNull
    public static KotlinType getUpperBoundsAsType(@NotNull TypeParameterDescriptor descriptor) {
        return intersectUpperBounds(descriptor, descriptor.getUpperBounds());
    }

    public static KotlinType intersectUpperBounds(@NotNull TypeParameterDescriptor descriptor, @NotNull List<KotlinType> upperBounds) {
        assert !upperBounds.isEmpty() : "Upper bound list is empty: " + descriptor;
        KotlinType upperBoundsAsType = intersectTypes(upperBounds);
        return upperBoundsAsType != null ? upperBoundsAsType : getBuiltIns(descriptor).getNothingType();
    }

    private static class TypeUnifier {
        private static class TypeParameterUsage {
            private final TypeParameterDescriptor typeParameterDescriptor;
            private final Variance howTheTypeParameterIsUsed;

            public TypeParameterUsage(TypeParameterDescriptor typeParameterDescriptor, Variance howTheTypeParameterIsUsed) {
                this.typeParameterDescriptor = typeParameterDescriptor;
                this.howTheTypeParameterIsUsed = howTheTypeParameterIsUsed;
            }
        }

        public static boolean mayBeEqual(@NotNull KotlinType type, @NotNull KotlinType other) {
            return unify(type, other);
        }

        private static boolean unify(KotlinType withParameters, KotlinType expected) {
            // T -> how T is used
            Map<TypeParameterDescriptor, Variance> parameters = new HashMap<>();
            Function1<TypeParameterUsage, Unit> processor = parameterUsage -> {
                Variance howTheTypeIsUsedBefore = parameters.get(parameterUsage.typeParameterDescriptor);
                if (howTheTypeIsUsedBefore == null) {
                    howTheTypeIsUsedBefore = Variance.INVARIANT;
                }
                parameters.put(parameterUsage.typeParameterDescriptor,
                               parameterUsage.howTheTypeParameterIsUsed.superpose(howTheTypeIsUsedBefore));
                return Unit.INSTANCE;
            };
            processAllTypeParameters(withParameters, Variance.INVARIANT, processor, parameters::containsKey);
            processAllTypeParameters(expected, Variance.INVARIANT, processor, parameters::containsKey);
            ConstraintSystem.Builder constraintSystem = new ConstraintSystemBuilderImpl();
            TypeSubstitutor substitutor = constraintSystem.registerTypeVariables(CallHandle.NONE.INSTANCE, parameters.keySet(), false);
            constraintSystem.addSubtypeConstraint(withParameters, substitutor.substitute(expected, Variance.INVARIANT), SPECIAL.position());

            return constraintSystem.build().getStatus().isSuccessful();
        }

        private static void processAllTypeParameters(
                KotlinType type,
                Variance howThisTypeIsUsed,
                Function1<TypeParameterUsage, Unit> result,
                Function1<TypeParameterDescriptor, Boolean> containsParameter
        ) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof TypeParameterDescriptor) {
                if (containsParameter.invoke((TypeParameterDescriptor) descriptor)) return;

                result.invoke(new TypeParameterUsage((TypeParameterDescriptor) descriptor, howThisTypeIsUsed));

                for (KotlinType superType : type.getConstructor().getSupertypes()) {
                    processAllTypeParameters(superType, howThisTypeIsUsed, result, containsParameter);
                }
            }
            for (TypeProjection projection : type.getArguments()) {
                if (projection.isStarProjection()) continue;
                processAllTypeParameters(projection.getType(), projection.getProjectionKind(), result, containsParameter);
            }
        }
    }
}
