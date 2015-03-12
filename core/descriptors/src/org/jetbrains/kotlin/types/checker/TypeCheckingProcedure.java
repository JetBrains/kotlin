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

package org.jetbrains.kotlin.types.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.types.*;

import java.util.List;

import static org.jetbrains.kotlin.types.Variance.*;

public class TypeCheckingProcedure {

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    public static JetType findCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
        return findCorrespondingSupertype(subtype, supertype, new TypeCheckerProcedureCallbacksImpl());
    }

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    public static JetType findCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype, @NotNull TypeCheckingProcedureCallbacks typeCheckingProcedureCallbacks) {
        TypeConstructor constructor = subtype.getConstructor();
        if (typeCheckingProcedureCallbacks.assertEqualTypeConstructors(constructor, supertype.getConstructor())) {
            return subtype;
        }
        for (JetType immediateSupertype : constructor.getSupertypes()) {
            JetType correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype, typeCheckingProcedureCallbacks);
            if (correspondingSupertype != null) {
                return TypeSubstitutor.create(subtype).safeSubstitute(correspondingSupertype, Variance.INVARIANT);
            }
        }
        return null;
    }

    public static JetType getOutType(TypeParameterDescriptor parameter, TypeProjection argument) {
        boolean isOutProjected = argument.getProjectionKind() == IN_VARIANCE || parameter.getVariance() == IN_VARIANCE;
        return isOutProjected ? parameter.getUpperBoundsAsType() : argument.getType();
    }

    public static JetType getInType(TypeParameterDescriptor parameter, TypeProjection argument) {
        boolean isOutProjected = argument.getProjectionKind() == OUT_VARIANCE || parameter.getVariance() == OUT_VARIANCE;
        return isOutProjected ? KotlinBuiltIns.getInstance().getNothingType() : argument.getType();
    }

    private final TypeCheckingProcedureCallbacks constraints;

    public TypeCheckingProcedure(TypeCheckingProcedureCallbacks constraints) {
        this.constraints = constraints;
    }

    public boolean equalTypes(@NotNull JetType type1, @NotNull JetType type2) {
        if (TypesPackage.isFlexible(type1)) {
            if (TypesPackage.isFlexible(type2)) {
                return !type1.isError() && !type2.isError() && isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1);
            }
            return heterogeneousEquivalence(type2, type1);
        }
        else if (TypesPackage.isFlexible(type2)) {
            return heterogeneousEquivalence(type1, type2);
        }

        if (type1.isMarkedNullable() != type2.isMarkedNullable()) {
            return false;
        }

        if (type1.isMarkedNullable()) {
            // Then type2 is nullable, too (see the previous condition
            return constraints.assertEqualTypes(TypeUtils.makeNotNullable(type1), TypeUtils.makeNotNullable(type2), this);
        }

        TypeConstructor constructor1 = type1.getConstructor();
        TypeConstructor constructor2 = type2.getConstructor();

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false;
        }

        List<TypeProjection> type1Arguments = type1.getArguments();
        List<TypeProjection> type2Arguments = type2.getArguments();
        if (type1Arguments.size() != type2Arguments.size()) {
            return false;
        }

        for (int i = 0; i < type1Arguments.size(); i++) {
            TypeParameterDescriptor typeParameter1 = constructor1.getParameters().get(i);
            TypeProjection typeProjection1 = type1Arguments.get(i);
            TypeParameterDescriptor typeParameter2 = constructor2.getParameters().get(i);
            TypeProjection typeProjection2 = type2Arguments.get(i);
            if (typeProjection1.isStarProjection() && typeProjection2.isStarProjection()) {
                continue;
            }

            if (capture(typeProjection1, typeProjection2, typeParameter1)) {
                continue;
            }
            if (getEffectiveProjectionKind(typeParameter1, typeProjection1) != getEffectiveProjectionKind(typeParameter2, typeProjection2)) {
                return false;
            }

            if (!constraints.assertEqualTypes(typeProjection1.getType(), typeProjection2.getType(), this)) {
                return false;
            }
        }
        return true;
    }

    protected boolean heterogeneousEquivalence(JetType inflexibleType, JetType flexibleType) {
        // This is to account for the case when we have Collection<X> vs (Mutable)Collection<X>! or K(java.util.Collection<? extends X>)
        assert !TypesPackage.isFlexible(inflexibleType) : "Only inflexible types are allowed here: " + inflexibleType;
        return isSubtypeOf(TypesPackage.flexibility(flexibleType).getLowerBound(), inflexibleType)
               && isSubtypeOf(inflexibleType, TypesPackage.flexibility(flexibleType).getUpperBound());
    }

    public enum EnrichedProjectionKind {
        IN, OUT, INV, STAR;

        @NotNull
        public static EnrichedProjectionKind fromVariance(@NotNull Variance variance) {
            switch (variance) {
                case INVARIANT:
                    return INV;
                case IN_VARIANCE:
                    return IN;
                case OUT_VARIANCE:
                    return OUT;
            }
            throw new IllegalStateException("Unknown variance");
        }
    }

    // If class C<out T> then C<T> and C<out T> mean the same
    // out * out = out
    // out * in  = *
    // out * inv = out
    //
    // in * out  = *
    // in * in   = in
    // in * inv  = in
    //
    // inv * out = out
    // inv * in  = out
    // inv * inv = inv
    public static EnrichedProjectionKind getEffectiveProjectionKind(
            @NotNull TypeParameterDescriptor typeParameter,
            @NotNull TypeProjection typeArgument
    ) {
        Variance a = typeParameter.getVariance();
        Variance b = typeArgument.getProjectionKind();

        // If they are not both invariant, let's make b not invariant for sure
        if (b == INVARIANT) {
            Variance t = a;
            a = b;
            b = t;
        }

        // Opposites yield STAR
        if (a == IN_VARIANCE && b == OUT_VARIANCE) {
            return EnrichedProjectionKind.STAR;
        }
        if (a == OUT_VARIANCE && b == IN_VARIANCE) {
            return EnrichedProjectionKind.STAR;
        }

        // If they are not opposite, return b, because b is either equal to a or b is in/out and a is inv
        return EnrichedProjectionKind.fromVariance(b);
    }

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
        if (TypesPackage.sameTypeConstructors(subtype, supertype)) {
            return !subtype.isMarkedNullable() || supertype.isMarkedNullable();
        }
        JetType subtypeRepresentative = TypesPackage.getSubtypeRepresentative(subtype);
        JetType supertypeRepresentative = TypesPackage.getSupertypeRepresentative(supertype);
        if (subtypeRepresentative != subtype || supertypeRepresentative != supertype) {
            // recursive invocation for possible chain of representatives
            return isSubtypeOf(subtypeRepresentative, supertypeRepresentative);
        }
        return isSubtypeOfForRepresentatives(subtype, supertype);
    }

    private boolean isSubtypeOfForRepresentatives(JetType subtype, JetType supertype) {
        if (subtype.isError() || supertype.isError()) {
            return true;
        }
        if (!supertype.isMarkedNullable() && subtype.isMarkedNullable()) {
            return false;
        }
        subtype = TypeUtils.makeNotNullable(subtype);
        supertype = TypeUtils.makeNotNullable(supertype);
        if (KotlinBuiltIns.isNothingOrNullableNothing(subtype)) {
            return true;
        }
        @Nullable JetType closestSupertype = findCorrespondingSupertype(subtype, supertype, constraints);
        if (closestSupertype == null) {
            return constraints.noCorrespondingSupertype(subtype, supertype); // if this returns true, there still isn't any supertype to continue with
        }

        return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
    }

    private boolean checkSubtypeForTheSameConstructor(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        assert constraints.assertEqualTypeConstructors(constructor, supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

        List<TypeProjection> subArguments = subtype.getArguments();
        List<TypeProjection> superArguments = supertype.getArguments();
        if (subArguments.size() != superArguments.size()) return false;

        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            TypeParameterDescriptor parameter = parameters.get(i);

            TypeProjection superArgument = superArguments.get(i);
            if (superArgument.isStarProjection()) continue;

            JetType superIn = getInType(parameter, superArgument);
            JetType superOut = getOutType(parameter, superArgument);

            TypeProjection subArgument = subArguments.get(i);
            JetType subIn = getInType(parameter, subArgument);
            JetType subOut = getOutType(parameter, subArgument);

            if (capture(subArgument, superArgument, parameter)) continue;

            boolean argumentIsErrorType = subArgument.getType().isError() || superArgument.getType().isError();
            if (!argumentIsErrorType && parameter.getVariance() == INVARIANT
                    && subArgument.getProjectionKind() == INVARIANT && superArgument.getProjectionKind() == INVARIANT) {
                if (!constraints.assertEqualTypes(subArgument.getType(), superArgument.getType(), this)) return false;
            }
            else {
                if (!constraints.assertSubtype(subOut, superOut, this)) return false;
                if (superArgument.getProjectionKind() != Variance.OUT_VARIANCE) {
                    if (!constraints.assertSubtype(superIn, subIn, this)) return false;
                }
                else {
                    assert KotlinBuiltIns.isNothing(superIn) : "In component must be Nothing for out-projection";
                }
            }
        }
        return true;
    }

    private boolean capture(
            @NotNull TypeProjection firstProjection,
            @NotNull TypeProjection secondProjection,
            @NotNull TypeParameterDescriptor parameter
    ) {
        // Capturing makes sense only for invariant classes
        if (parameter.getVariance() != INVARIANT) return false;

        // Now, both subtype and supertype relations transform to equality constraints on type arguments:
        // Array<T> is a subtype, supertype or equal to Array<out Int> then T captures a type that extends Int: 'Captured(out Int)'
        // Array<T> is a subtype, supertype or equal to Array<in Int> then T captures a type that extends Int: 'Captured(in Int)'

        if (firstProjection.getProjectionKind() == INVARIANT && secondProjection.getProjectionKind() != INVARIANT) {
            return constraints.capture(firstProjection.getType(), secondProjection);
        }
        if (firstProjection.getProjectionKind() != INVARIANT && secondProjection.getProjectionKind() == INVARIANT) {
            return constraints.capture(secondProjection.getType(), firstProjection);
        }
        return false;
    }
}
