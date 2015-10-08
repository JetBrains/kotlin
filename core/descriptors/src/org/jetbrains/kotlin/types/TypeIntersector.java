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
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.kotlin.resolve.scopes.ChainedScope;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.resolve.calls.inference.InferencePackage.registerTypeVariables;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.SPECIAL;

public class TypeIntersector {

    public static boolean isIntersectionEmpty(@NotNull JetType typeA, @NotNull JetType typeB) {
        return intersectTypes(JetTypeChecker.DEFAULT, new LinkedHashSet<JetType>(Arrays.asList(typeA, typeB))) == null;
    }

    @Nullable
    public static JetType intersectTypes(
            @NotNull JetTypeChecker typeChecker,
            @NotNull Set<JetType> types
    ) {
        assert (!types.isEmpty()) : "Attempting to intersect empty set of types, this case should be dealt with on the call site.";

        if (types.size() == 1) {
            return types.iterator().next();
        }

        // Intersection of T1..Tn is an intersection of their non-null versions,
        //   made nullable is they all were nullable
        JetType nothingOrNullableNothing = null;
        boolean allNullable = true;
        List<JetType> nullabilityStripped = new ArrayList<JetType>(types.size());
        for (JetType type : types) {
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
            return ErrorUtils.createErrorType("Intersection of errors types: " + types);
        }

        // Now we remove types that have subtypes in the list
        List<JetType> resultingTypes = new ArrayList<JetType>();
        outer:
        for (JetType type : nullabilityStripped) {
            if (!TypeUtils.canHaveSubtypes(typeChecker, type)) {
                for (JetType other : nullabilityStripped) {
                    // It makes sense to check for subtyping (other <: type), despite that
                    // type is not supposed to be open, for there're enums
                    if (!TypeUnifier.mayBeEqual(type, other) && !typeChecker.isSubtypeOf(type, other) && !typeChecker.isSubtypeOf(other, type)) {
                        return null;
                    }
                }
                return TypeUtils.makeNullableAsSpecified(type, allNullable);
            }
            else {
                for (JetType other : nullabilityStripped) {
                    if (!type.equals(other) && typeChecker.isSubtypeOf(other, type)) {
                        continue outer;
                    }

                }
            }

            // Don't add type if it is already present, to avoid trivial type intersections in result
            for (JetType other : resultingTypes) {
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
            JetType bestRepresentative = TypesPackage.singleBestRepresentative(nullabilityStripped);
            if (bestRepresentative == null) {
                throw new AssertionError("Empty intersection for types " + types);
            }
            return TypeUtils.makeNullableAsSpecified(bestRepresentative, allNullable);
        }

        if (resultingTypes.size() == 1) {
            return TypeUtils.makeNullableAsSpecified(resultingTypes.get(0), allNullable);
        }

        TypeConstructor constructor = new IntersectionTypeConstructor(Annotations.Companion.getEMPTY(), resultingTypes);

        JetScope[] scopes = new JetScope[resultingTypes.size()];
        int i = 0;
        for (JetType type : resultingTypes) {
            scopes[i] = type.getMemberScope();
            i++;
        }

        return JetTypeImpl.create(
                Annotations.Companion.getEMPTY(),
                constructor,
                allNullable,
                Collections.<TypeProjection>emptyList(),
                new IntersectionScope(constructor, scopes)
        );
    }

    // TODO : check intersectibility, don't use a chanied scope
    private static class IntersectionScope extends ChainedScope {
        public IntersectionScope(@NotNull TypeConstructor constructor, @NotNull JetScope[] scopes) {
            super(null, "member scope for intersection type " + constructor, scopes);
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            throw new UnsupportedOperationException("Should not call getContainingDeclaration on intersection scope " + this);
        }
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

        public static boolean mayBeEqual(@NotNull JetType type, @NotNull JetType other) {
            return unify(type, other);
        }

        private static boolean unify(JetType withParameters, JetType expected) {
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
                    return Unit.INSTANCE$;
                }
            };
            processAllTypeParameters(withParameters, Variance.INVARIANT, processor);
            processAllTypeParameters(expected, Variance.INVARIANT, processor);
            ConstraintSystemImpl constraintSystem = new ConstraintSystemImpl();
            registerTypeVariables(constraintSystem, parameters);
            constraintSystem.addSubtypeConstraint(withParameters, expected, SPECIAL.position());

            return constraintSystem.getStatus().isSuccessful();
        }

        private static void processAllTypeParameters(JetType type, Variance howThisTypeIsUsed, Function1<TypeParameterUsage, Unit> result) {
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
