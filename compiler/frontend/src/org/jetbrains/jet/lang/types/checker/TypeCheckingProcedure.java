/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;

import static org.jetbrains.jet.lang.types.Variance.*;

/**
* @author abreslav
*/
public class TypeCheckingProcedure {

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    public static JetType findCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        if (constructor.equals(supertype.getConstructor())) {
            return subtype;
        }
        for (JetType immediateSupertype : constructor.getSupertypes()) {
            JetType correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype);
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

    private final TypingConstraints constraints;

    public TypeCheckingProcedure(TypingConstraints constraints) {
        this.constraints = constraints;
    }

    public boolean equalTypes(@NotNull JetType type1, @NotNull JetType type2) {
        if (type1.isNullable() != type2.isNullable()) {
            return false;
        }

        if (type1.isNullable()) {
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
            if (getEffectiveProjectionKind(typeParameter1, typeProjection1) != getEffectiveProjectionKind(typeParameter2, typeProjection2)) {
                return false;
            }

            if (!constraints.assertEqualTypes(typeProjection1.getType(), typeProjection2.getType(), this)) {
                return false;
            }
        }
        return true;
    }

    private enum EnrichedProjectionKind {
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
    private EnrichedProjectionKind getEffectiveProjectionKind(@NotNull TypeParameterDescriptor typeParameter, @NotNull TypeProjection typeArgument) {
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
        if (ErrorUtils.isErrorType(subtype) || ErrorUtils.isErrorType(supertype)) {
            return true;
        }
        if (!supertype.isNullable() && subtype.isNullable()) {
            return false;
        }
        subtype = TypeUtils.makeNotNullable(subtype);
        supertype = TypeUtils.makeNotNullable(supertype);
        if (KotlinBuiltIns.getInstance().isNothingOrNullableNothing(subtype)) {
            return true;
        }
        @Nullable JetType closestSupertype = findCorrespondingSupertype(subtype, supertype);
        if (closestSupertype == null) {
            return constraints.noCorrespondingSupertype(subtype, supertype); // if this returns true, there still isn't any supertype to continue with
        }

        return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
    }

    private boolean checkSubtypeForTheSameConstructor(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        assert constructor.equals(supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

        List<TypeProjection> subArguments = subtype.getArguments();
        List<TypeProjection> superArguments = supertype.getArguments();
        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            TypeParameterDescriptor parameter = parameters.get(i);


            TypeProjection subArgument = subArguments.get(i);
            JetType subIn = getInType(parameter, subArgument);
            JetType subOut = getOutType(parameter, subArgument);

            TypeProjection superArgument = superArguments.get(i);
            JetType superIn = getInType(parameter, superArgument);
            JetType superOut = getOutType(parameter, superArgument);

            if (parameter.getVariance() == INVARIANT && subArgument.getProjectionKind() == INVARIANT && superArgument.getProjectionKind() == INVARIANT) {
                if (!constraints.assertEqualTypes(subArgument.getType(), superArgument.getType(), this)) return false;
            }
            else {
                if (!constraints.assertSubtype(subOut, superOut, this)) return false;
                if (!constraints.assertSubtype(superIn, subIn, this)) return false;
            }
        }
        return true;
    }
}
