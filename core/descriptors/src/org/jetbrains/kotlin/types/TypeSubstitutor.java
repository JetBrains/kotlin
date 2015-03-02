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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.resolve.calls.inference.InferencePackage;
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilPackage;
import org.jetbrains.kotlin.types.typesApproximation.TypesApproximationPackage;

import java.util.*;

public class TypeSubstitutor {

    private static final int MAX_RECURSION_DEPTH = 100;

    public static class MapToTypeSubstitutionAdapter extends TypeSubstitution {
        private final @NotNull Map<TypeConstructor, TypeProjection> substitutionContext;

        public MapToTypeSubstitutionAdapter(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
            this.substitutionContext = substitutionContext;
        }

        @Override
        public TypeProjection get(TypeConstructor key) {
            return substitutionContext.get(key);
        }

        @Override
        public boolean isEmpty() {
            return substitutionContext.isEmpty();
        }

        @Override
        public String toString() {
            return substitutionContext.toString();
        }
    }

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
    public static TypeSubstitutor create(@NotNull TypeSubstitution... substitutions) {
        return create(new CompositeTypeSubstitution(substitutions));
    }

    @NotNull
    public static TypeSubstitutor create(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
        return create(new MapToTypeSubstitutionAdapter(substitutionContext));
    }

    @NotNull
    public static TypeSubstitutor create(@NotNull JetType context) {
        return create(buildSubstitutionContext(context.getConstructor().getParameters(), context.getArguments()));
    }

    @NotNull
    public static Map<TypeConstructor, TypeProjection> buildSubstitutionContext(
            @NotNull List<TypeParameterDescriptor> parameters,
            @NotNull List<? extends TypeProjection> contextArguments
    ) {
        Map<TypeConstructor, TypeProjection> parameterValues = new HashMap<TypeConstructor, TypeProjection>();
        if (parameters.size() != contextArguments.size()) {
            throw new IllegalArgumentException("type parameter count != context arguments: \n" +
                                               "parameters=" + parameters + "\n" +
                                               "contextArgs=" + contextArguments);
        }
        for (int i = 0, size = parameters.size(); i < size; i++) {
            parameterValues.put(parameters.get(i).getTypeConstructor(), contextArguments.get(i));
        }
        return parameterValues;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final @NotNull TypeSubstitution substitution;

    protected TypeSubstitutor(@NotNull TypeSubstitution substitution) {
        this.substitution = substitution;
    }

    public boolean inRange(@NotNull TypeConstructor typeConstructor) {
        return substitution.get(typeConstructor) != null;
    }

    public boolean isEmpty() {
        return substitution.isEmpty();
    }

    @NotNull
    public TypeSubstitution getSubstitution() {
        return substitution;
    }

    @NotNull
    public JetType safeSubstitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
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
    public JetType substitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
        TypeProjection projection = substitute(new TypeProjectionImpl(howThisTypeIsUsed, type));
        return projection == null ? null : projection.getType();
    }

    @Nullable
    public TypeProjection substitute(@NotNull TypeProjection typeProjection) {
        TypeProjection substitutedTypeProjection = substituteWithoutApproximation(typeProjection);
        if (!substitution.approximateCapturedTypes()) {
            return substitutedTypeProjection;
        }
        return TypesApproximationPackage.approximateCapturedTypesIfNecessary(substitutedTypeProjection);
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
        JetType type = originalProjection.getType();
        Variance originalProjectionKind = originalProjection.getProjectionKind();
        if (TypesPackage.isFlexible(type) && !TypesPackage.isCustomTypeVariable(type)) {
            Flexibility flexibility = TypesPackage.flexibility(type);
            TypeProjection substitutedLower =
                    unsafeSubstitute(new TypeProjectionImpl(originalProjectionKind, flexibility.getLowerBound()), recursionDepth + 1);
            TypeProjection substitutedUpper =
                    unsafeSubstitute(new TypeProjectionImpl(originalProjectionKind, flexibility.getUpperBound()), recursionDepth + 1);

            Variance substitutedProjectionKind = substitutedLower.getProjectionKind();
            assert (substitutedProjectionKind == substitutedUpper.getProjectionKind()) &&
                   originalProjectionKind == Variance.INVARIANT || originalProjectionKind == substitutedProjectionKind :
                    "Unexpected substituted projection kind: " + substitutedProjectionKind + "; original: " + originalProjectionKind;

            JetType substitutedFlexibleType = DelegatingFlexibleType.create(
                    substitutedLower.getType(), substitutedUpper.getType(), flexibility.getExtraCapabilities());
            return new TypeProjectionImpl(substitutedProjectionKind, substitutedFlexibleType);
        }

        if (KotlinBuiltIns.isNothing(type) || type.isError()) return originalProjection;

        TypeProjection replacement = substitution.get(type.getConstructor());

        if (replacement != null) {
            VarianceConflictType varianceConflict = conflictType(originalProjectionKind, replacement.getProjectionKind());

            // Captured type might be substituted in an opposite projection:
            // out 'Captured (in Int)' = out Int
            // in 'Captured (out Int)' = in Int
            boolean allowVarianceConflict = InferencePackage.isCaptured(type);
            if (!allowVarianceConflict) {
                //noinspection EnumSwitchStatementWhichMissesCases
                switch (varianceConflict) {
                    case OUT_IN_IN_POSITION:
                        throw new SubstitutionException("Out-projection in in-position");
                    case IN_IN_OUT_POSITION:
                        // todo use the right type parameter variance and upper bound
                        return new TypeProjectionImpl(Variance.OUT_VARIANCE, KotlinBuiltIns.getInstance().getNullableAnyType());
                }
            }
            JetType substitutedType;
            CustomTypeVariable typeVariable = TypesPackage.getCustomTypeVariable(type);
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

            Variance resultingProjectionKind = varianceConflict == VarianceConflictType.NO_CONFLICT
                                               ? combine(originalProjectionKind, replacement.getProjectionKind())
                                               : originalProjectionKind;
            return new TypeProjectionImpl(resultingProjectionKind, substitutedType);
        }
        // The type is not within the substitution range, i.e. Foo, Bar<T> etc.
        return substituteCompoundType(originalProjection, recursionDepth);
    }

    private TypeProjection substituteCompoundType(
            TypeProjection originalProjection,
            int recursionDepth
    ) throws SubstitutionException {
        final JetType type = originalProjection.getType();
        Variance projectionKind = originalProjection.getProjectionKind();
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            // substitution can't change type parameter
            // todo substitute bounds
            return originalProjection;
        }

        List<TypeProjection> substitutedArguments = substituteTypeArguments(
                type.getConstructor().getParameters(), type.getArguments(), recursionDepth);

        // Only type parameters of the corresponding class (or captured type parameters of outer declaration) are substituted
        // e.g. for return type Foo of 'add(..)' in 'class Foo { fun <R> add(bar: Bar<R>): Foo }' R shouldn't be substituted in the scope
        TypeSubstitution substitutionFilteringTypeParameters = new TypeSubstitution() {
            private final Collection<TypeConstructor> containedOrCapturedTypeParameters =
                    TypeUtilPackage.getContainedAndCapturedTypeParameterConstructors(type);

            @Nullable
            @Override
            public TypeProjection get(TypeConstructor key) {
                return containedOrCapturedTypeParameters.contains(key) ? substitution.get(key) : null;
            }

            @Override
            public boolean isEmpty() {
                return substitution.isEmpty();
            }
        };
        JetType substitutedType = new JetTypeImpl(type.getAnnotations(),   // Old annotations. This is questionable
                                           type.getConstructor(),   // The same constructor
                                           type.isMarkedNullable(),       // Same nullability
                                           substitutedArguments,
                                           new SubstitutingScope(type.getMemberScope(), create(substitutionFilteringTypeParameters)));
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
            if (e.getClass().getName().equals("com.intellij.openapi.progress.ProcessCanceledException")) {
                //noinspection ConstantConditions
                throw (RuntimeException) e;
            }
            return "[Exception while computing toString(): " + e + "]";
        }
    }
}
