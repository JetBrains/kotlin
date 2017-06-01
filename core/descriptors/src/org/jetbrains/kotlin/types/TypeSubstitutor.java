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

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations;
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructorKt;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;
import org.jetbrains.kotlin.types.typesApproximation.CapturedTypeApproximationKt;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TypeSubstitutor {

    private static final int MAX_RECURSION_DEPTH = 100;

    public static final TypeSubstitutor EMPTY = create(TypeSubstitution.EMPTY);

    private static final class SubstitutionException extends Exception {
        public SubstitutionException(String message) {
            super(message);
        }
    }

    @NotNull
    public static TypeSubstitutor create(@NotNull TypeSubstitution substitution) {
        return new TypeSubstitutor(substitution);
    }

    @NotNull
    public static TypeSubstitutor createChainedSubstitutor(@NotNull TypeSubstitution first, @NotNull TypeSubstitution second) {
        return create(DisjointKeysUnionTypeSubstitution.create(first, second));
    }

    @NotNull
    public static TypeSubstitutor create(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
        return create(TypeConstructorSubstitution.createByConstructorsMap(substitutionContext));
    }

    @NotNull
    public static TypeSubstitutor create(@NotNull KotlinType context) {
        return create(TypeConstructorSubstitution.create(context.getConstructor(), context.getArguments()));
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final @NotNull TypeSubstitution substitution;

    protected TypeSubstitutor(@NotNull TypeSubstitution substitution) {
        this.substitution = substitution;
    }

    public boolean isEmpty() {
        return substitution.isEmpty();
    }

    @NotNull
    public TypeSubstitution getSubstitution() {
        return substitution;
    }

    @NotNull
    public KotlinType safeSubstitute(@NotNull KotlinType type, @NotNull Variance howThisTypeIsUsed) {
        if (isEmpty()) {
            return type;
        }

        try {
            return unsafeSubstitute(new TypeProjectionImpl(howThisTypeIsUsed, type), 0).getType();
        } catch (SubstitutionException e) {
            return ErrorUtils.createErrorType(e.getMessage());
        }
    }

    @Nullable
    public KotlinType substitute(@NotNull KotlinType type, @NotNull Variance howThisTypeIsUsed) {
        TypeProjection projection =
                substitute(new TypeProjectionImpl(howThisTypeIsUsed, getSubstitution().prepareTopLevelType(type, howThisTypeIsUsed)));
        return projection == null ? null : projection.getType();
    }

    @Nullable
    public TypeProjection substitute(@NotNull TypeProjection typeProjection) {
        TypeProjection substitutedTypeProjection = substituteWithoutApproximation(typeProjection);
        if (!substitution.approximateCapturedTypes() && !substitution.approximateContravariantCapturedTypes()) {
            return substitutedTypeProjection;
        }
        return CapturedTypeApproximationKt.approximateCapturedTypesIfNecessary(
                substitutedTypeProjection, substitution.approximateContravariantCapturedTypes());
    }

    @Nullable
    public TypeProjection substituteWithoutApproximation(@NotNull TypeProjection typeProjection) {
        if (isEmpty()) {
            return typeProjection;
        }

        try {
            return unsafeSubstitute(typeProjection, 0);
        } catch (SubstitutionException e) {
            return null;
        }
    }

    @NotNull
    private TypeProjection unsafeSubstitute(@NotNull TypeProjection originalProjection, int recursionDepth) throws SubstitutionException {
        assertRecursionDepth(recursionDepth, originalProjection, substitution);

        if (originalProjection.isStarProjection()) return originalProjection;

        // The type is within the substitution range, i.e. T or T?
        KotlinType type = originalProjection.getType();
        if (DynamicTypesKt.isDynamic(type) || type.unwrap() instanceof RawType) {
            return originalProjection; // todo investigate
        }

        TypeProjection replacement = substitution.get(type);
        Variance originalProjectionKind = originalProjection.getProjectionKind();
        if (replacement == null && FlexibleTypesKt.isFlexible(type) && !TypeCapabilitiesKt.isCustomTypeVariable(type)) {
            FlexibleType flexibleType = FlexibleTypesKt.asFlexibleType(type);
            TypeProjection substitutedLower =
                    unsafeSubstitute(new TypeProjectionImpl(originalProjectionKind, flexibleType.getLowerBound()), recursionDepth + 1);
            TypeProjection substitutedUpper =
                    unsafeSubstitute(new TypeProjectionImpl(originalProjectionKind, flexibleType.getUpperBound()), recursionDepth + 1);

            Variance substitutedProjectionKind = substitutedLower.getProjectionKind();
            assert (substitutedProjectionKind == substitutedUpper.getProjectionKind()) &&
                   originalProjectionKind == Variance.INVARIANT || originalProjectionKind == substitutedProjectionKind :
                    "Unexpected substituted projection kind: " + substitutedProjectionKind + "; original: " + originalProjectionKind;

            KotlinType substitutedFlexibleType = KotlinTypeFactory.flexibleType(
                    TypeSubstitutionKt.asSimpleType(substitutedLower.getType()), TypeSubstitutionKt.asSimpleType(substitutedUpper.getType()));
            return new TypeProjectionImpl(substitutedProjectionKind, substitutedFlexibleType);
        }

        if (KotlinBuiltIns.isNothing(type) || KotlinTypeKt.isError(type)) return originalProjection;

        if (replacement != null) {
            VarianceConflictType varianceConflict = conflictType(originalProjectionKind, replacement.getProjectionKind());

            // Captured type might be substituted in an opposite projection:
            // out 'Captured (in Int)' = out Int
            // in 'Captured (out Int)' = in Int
            boolean allowVarianceConflict = CapturedTypeConstructorKt.isCaptured(type);
            if (!allowVarianceConflict) {
                //noinspection EnumSwitchStatementWhichMissesCases
                switch (varianceConflict) {
                    case OUT_IN_IN_POSITION:
                        throw new SubstitutionException("Out-projection in in-position");
                    case IN_IN_OUT_POSITION:
                        // todo use the right type parameter variance and upper bound
                        return new TypeProjectionImpl(Variance.OUT_VARIANCE, type.getConstructor().getBuiltIns().getNullableAnyType());
                }
            }
            KotlinType substitutedType;
            CustomTypeVariable typeVariable = TypeCapabilitiesKt.getCustomTypeVariable(type);
            if (replacement.isStarProjection()) {
                return replacement;
            }
            else if (typeVariable != null) {
                substitutedType = typeVariable.substitutionResult(replacement.getType());
            }
            else {
                // this is a simple type T or T?: if it's T, we should just take replacement, if T? - we make replacement nullable
                substitutedType = TypeUtils.makeNullableIfNeeded(replacement.getType(), type.isMarkedNullable());
            }

            // substitutionType.annotations = replacement.annotations ++ type.annotations
            if (!type.getAnnotations().isEmpty()) {
                Annotations typeAnnotations = filterOutUnsafeVariance(substitution.filterAnnotations(type.getAnnotations()));
                substitutedType = TypeUtilsKt.replaceAnnotations(
                        substitutedType,
                        new CompositeAnnotations(substitutedType.getAnnotations(), typeAnnotations)
                );
            }

            Variance resultingProjectionKind = varianceConflict == VarianceConflictType.NO_CONFLICT
                                               ? combine(originalProjectionKind, replacement.getProjectionKind())
                                               : originalProjectionKind;
            return new TypeProjectionImpl(resultingProjectionKind, substitutedType);
        }
        // The type is not within the substitution range, i.e. Foo, Bar<T> etc.
        return substituteCompoundType(originalProjection, recursionDepth);
    }

    @NotNull
    private static Annotations filterOutUnsafeVariance(@NotNull Annotations annotations) {
        if (!annotations.hasAnnotation(KotlinBuiltIns.FQ_NAMES.unsafeVariance)) return annotations;
        return new FilteredAnnotations(annotations, new Function1<FqName, Boolean>() {
            @Override
            public Boolean invoke(@NotNull  FqName name) {
                return !name.equals(KotlinBuiltIns.FQ_NAMES.unsafeVariance);
            }
        });
    }

    private TypeProjection substituteCompoundType(
            TypeProjection originalProjection,
            int recursionDepth
    ) throws SubstitutionException {
        KotlinType type = originalProjection.getType();
        Variance projectionKind = originalProjection.getProjectionKind();
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            // substitution can't change type parameter
            // todo substitute bounds
            return originalProjection;
        }

        KotlinType substitutedAbbreviation = null;
        SimpleType abbreviation = SpecialTypesKt.getAbbreviation(type);
        if (abbreviation != null) {
            substitutedAbbreviation = substitute(abbreviation, Variance.INVARIANT);
        }

        List<TypeProjection> substitutedArguments = substituteTypeArguments(
                type.getConstructor().getParameters(), type.getArguments(), recursionDepth);

        KotlinType substitutedType =
                TypeSubstitutionKt.replace(type, substitutedArguments, substitution.filterAnnotations(type.getAnnotations()));
        if (substitutedType instanceof SimpleType && substitutedAbbreviation instanceof SimpleType) {
            substitutedType = SpecialTypesKt.withAbbreviation((SimpleType) substitutedType, (SimpleType) substitutedAbbreviation);
        }

        return new TypeProjectionImpl(projectionKind, substitutedType);
    }

    private List<TypeProjection> substituteTypeArguments(
            List<TypeParameterDescriptor> typeParameters, List<TypeProjection> typeArguments, int recursionDepth
    ) throws SubstitutionException {
        List<TypeProjection> substitutedArguments = new ArrayList<TypeProjection>(typeParameters.size());
        for (int i = 0; i < typeParameters.size(); i++) {
            TypeParameterDescriptor typeParameter = typeParameters.get(i);
            TypeProjection typeArgument = typeArguments.get(i);

            TypeProjection substitutedTypeArgument = unsafeSubstitute(typeArgument, recursionDepth + 1);

            switch (conflictType(typeParameter.getVariance(), substitutedTypeArgument.getProjectionKind())) {
                case NO_CONFLICT:
                    // if the corresponding type parameter is already co/contra-variant, there's not need for an explicit projection
                    if (typeParameter.getVariance() != Variance.INVARIANT && !substitutedTypeArgument.isStarProjection()) {
                        substitutedTypeArgument = new TypeProjectionImpl(Variance.INVARIANT, substitutedTypeArgument.getType());
                    }
                    break;
                case OUT_IN_IN_POSITION:
                case IN_IN_OUT_POSITION:
                    substitutedTypeArgument = TypeUtils.makeStarProjection(typeParameter);
                    break;
            }

            substitutedArguments.add(substitutedTypeArgument);
        }
        return substitutedArguments;
    }

    @NotNull
    public static Variance combine(@NotNull Variance typeParameterVariance, @NotNull TypeProjection typeProjection) {
        if (typeProjection.isStarProjection()) return Variance.OUT_VARIANCE;

        return combine(typeParameterVariance, typeProjection.getProjectionKind());
    }

    @NotNull
    public static Variance combine(@NotNull Variance typeParameterVariance, @NotNull Variance projectionKind) {
        if (typeParameterVariance == Variance.INVARIANT) return projectionKind;
        if (projectionKind == Variance.INVARIANT) return typeParameterVariance;
        if (typeParameterVariance == projectionKind) return projectionKind;
        throw new AssertionError("Variance conflict: type parameter variance '" + typeParameterVariance + "' and " +
                                 "projection kind '" + projectionKind + "' cannot be combined");
    }

    private enum VarianceConflictType {
        NO_CONFLICT,
        IN_IN_OUT_POSITION,
        OUT_IN_IN_POSITION
    }

    private static VarianceConflictType conflictType(Variance position, Variance argument) {
        if (position == Variance.IN_VARIANCE && argument == Variance.OUT_VARIANCE) {
            return VarianceConflictType.OUT_IN_IN_POSITION;
        }
        if (position == Variance.OUT_VARIANCE && argument == Variance.IN_VARIANCE) {
            return VarianceConflictType.IN_IN_OUT_POSITION;
        }
        return VarianceConflictType.NO_CONFLICT;
    }

    private static void assertRecursionDepth(int recursionDepth, TypeProjection projection, TypeSubstitution substitution) {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            throw new IllegalStateException("Recursion too deep. Most likely infinite loop while substituting " + safeToString(projection) + "; substitution: " + safeToString(substitution));
        }
    }

    private static String safeToString(Object o) {
        try {
            return o.toString();
        }
        catch (Throwable e) {
            if (ExceptionUtilsKt.isProcessCanceledException(e)) {
                //noinspection ConstantConditions
                throw (RuntimeException) e;
            }
            return "[Exception while computing toString(): " + e + "]";
        }
    }
}
