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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl;
import org.jetbrains.kotlin.resolve.scopes.ChainedScope;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.SPECIAL;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class TypeIntersector {

    public static boolean isIntersectionEmpty(@NotNull KotlinType typeA, @NotNull KotlinType typeB) {
        return intersectTypes(KotlinTypeChecker.DEFAULT, new LinkedHashSet<KotlinType>(Arrays.asList(typeA, typeB))) == null;
    }

    @Nullable
    public static KotlinType intersectTypes(@NotNull KotlinTypeChecker typeChecker, @NotNull Collection<KotlinType> types) {
        assert !types.isEmpty() : "Attempting to intersect empty collection of types, this case should be dealt with on the call site.";

        if (types.size() == 1) {
            return types.iterator().next();
        }

        // Intersection of T1..Tn is an intersection of their non-null versions,
        //   made nullable is they all were nullable
        KotlinType nothingOrNullableNothing = null;
        boolean allNullable = true;
        List<KotlinType> nullabilityStripped = new ArrayList<KotlinType>(types.size());
        for (KotlinType type : types) {
            if (type.isError()) continue;

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
            return ErrorUtils.createErrorType("Intersection of error types: " + types);
        }

        // Now we remove types that have subtypes in the list
        List<KotlinType> resultingTypes = new ArrayList<KotlinType>();
        outer:
        for (KotlinType type : nullabilityStripped) {
            if (!TypeUtils.canHaveSubtypes(typeChecker, type)) {
                for (KotlinType other : nullabilityStripped) {
                    // It makes sense to check for subtyping (other <: type), despite that
                    // type is not supposed to be open, for there're enums
                    if (!TypeUnifier.mayBeEqual(type, other) && !typeChecker.isSubtypeOf(type, other) && !typeChecker.isSubtypeOf(other, type)) {
                        return null;
                    }
                }
                return TypeUtils.makeNullableAsSpecified(type, allNullable);
            }
            else {
                for (KotlinType other : nullabilityStripped) {
                    if (!type.equals(other) && typeChecker.isSubtypeOf(other, type)) {
                        continue outer;
                    }
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
                throw new AssertionError("Empty intersection for types " + types);
            }
            return TypeUtils.makeNullableAsSpecified(bestRepresentative, allNullable);
        }

        if (resultingTypes.size() == 1) {
            return TypeUtils.makeNullableAsSpecified(resultingTypes.get(0), allNullable);
        }

        TypeConstructor constructor = new IntersectionTypeConstructor(Annotations.Companion.getEMPTY(), resultingTypes);

        MemberScope[] scopes = new MemberScope[resultingTypes.size()];
        int i = 0;
        for (KotlinType type : resultingTypes) {
            scopes[i] = type.getMemberScope();
            i++;
        }

        return KotlinTypeImpl.create(
                Annotations.Companion.getEMPTY(),
                constructor,
                allNullable,
                Collections.<TypeProjection>emptyList(),
                new ChainedScope("member scope for intersection type " + constructor, scopes)
        );
    }

    /**
     * Note: this method was used in overload and override bindings to approximate type parameters with several bounds,
     * but as it turned out at some point, that logic was inconsistent with Java rules, so it was simplified.
     * Most of the other usages of this method are left untouched but probably should be investigated closely if they're still valid.
     */
    @NotNull
    public static KotlinType getUpperBoundsAsType(@NotNull TypeParameterDescriptor descriptor) {
        List<KotlinType> upperBounds = descriptor.getUpperBounds();
        assert !upperBounds.isEmpty() : "Upper bound list is empty: " + descriptor;
        KotlinType upperBoundsAsType = intersectTypes(KotlinTypeChecker.DEFAULT, upperBounds);
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
            final Map<TypeParameterDescriptor, Variance> parameters = new HashMap<TypeParameterDescriptor, Variance>();
            Function1<TypeParameterUsage, Unit> processor = new Function1<TypeParameterUsage, Unit>() {
                @Override
                public Unit invoke(TypeParameterUsage parameterUsage) {
                    Variance howTheTypeIsUsedBefore = parameters.get(parameterUsage.typeParameterDescriptor);
                    if (howTheTypeIsUsedBefore == null) {
                        howTheTypeIsUsedBefore = Variance.INVARIANT;
                    }
                    parameters.put(parameterUsage.typeParameterDescriptor,
                                   parameterUsage.howTheTypeParameterIsUsed.superpose(howTheTypeIsUsedBefore));
                    return Unit.INSTANCE;
                }
            };
            processAllTypeParameters(withParameters, Variance.INVARIANT, processor);
            processAllTypeParameters(expected, Variance.INVARIANT, processor);
            ConstraintSystem.Builder constraintSystem = new ConstraintSystemBuilderImpl();
            constraintSystem.registerTypeVariables(parameters.keySet(), new Function1<TypeParameterDescriptor, TypeParameterDescriptor>() {
                @Override
                public TypeParameterDescriptor invoke(TypeParameterDescriptor descriptor) {
                    return descriptor;
                }
            }, false);
            constraintSystem.addSubtypeConstraint(withParameters, expected, SPECIAL.position());

            return constraintSystem.build().getStatus().isSuccessful();
        }

        private static void processAllTypeParameters(KotlinType type, Variance howThisTypeIsUsed, Function1<TypeParameterUsage, Unit> result) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof TypeParameterDescriptor) {
                result.invoke(new TypeParameterUsage((TypeParameterDescriptor) descriptor, howThisTypeIsUsed));
            }
            for (TypeProjection projection : type.getArguments()) {
                if (projection.isStarProjection()) continue;
                processAllTypeParameters(projection.getType(), projection.getProjectionKind(), result);
            }
        }
    }
}
