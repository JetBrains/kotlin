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
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeSubstitutor {

    private static final int MAX_RECURSION_DEPTH = 100;

    public static class MapToTypeSubstitutionAdapter implements TypeSubstitution {
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

    public static TypeSubstitutor create(@NotNull TypeSubstitution substitution) {
        return new TypeSubstitutor(substitution);
    }

    public static TypeSubstitutor create(@NotNull TypeSubstitution... substitutions) {
        return create(new CompositeTypeSubstitution(substitutions));
    }

    public static TypeSubstitutor create(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
        return create(new MapToTypeSubstitutionAdapter(substitutionContext));
    }

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
        // The type is within the substitution range, i.e. T or T?
        JetType type = originalProjection.getType();
        if (KotlinBuiltIns.getInstance().isNothing(type) || type.isError()) return originalProjection;

        TypeProjection replacement = substitution.get(type.getConstructor());

        if (replacement != null) {
            // It must be a type parameter: only they can be directly substituted for
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor();

            switch (conflictType(originalProjection.getProjectionKind(), replacement.getProjectionKind())) {
                case OUT_IN_IN_POSITION:
                    throw new SubstitutionException("Out-projection in in-position");
                case IN_IN_OUT_POSITION:
                    //noinspection ConstantConditions
                    return TypeUtils.makeStarProjection(typeParameter);
                case NO_CONFLICT:
                    boolean resultingIsNullable = type.isNullable() || replacement.getType().isNullable();
                    JetType substitutedType = TypeUtils.makeNullableAsSpecified(replacement.getType(), resultingIsNullable);
                    Variance resultingProjectionKind = combine(originalProjection.getProjectionKind(), replacement.getProjectionKind());

                    return new TypeProjectionImpl(resultingProjectionKind, substitutedType);
                default:
                    throw new IllegalStateException();
            }
        }
        else {
            // The type is not within the substitution range, i.e. Foo, Bar<T> etc.
            List<TypeProjection> substitutedArguments = substituteTypeArguments(
                    type.getConstructor().getParameters(), type.getArguments(), recursionDepth);

            JetType substitutedType = new JetTypeImpl(type.getAnnotations(),   // Old annotations. This is questionable
                                               type.getConstructor(),   // The same constructor
                                               type.isNullable(),       // Same nullability
                                               substitutedArguments,
                                               new SubstitutingScope(type.getMemberScope(), this));
            return new TypeProjectionImpl(originalProjection.getProjectionKind(), substitutedType);
        }
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
                    if (typeParameter.getVariance() != Variance.INVARIANT) {
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

    private static Variance combine(Variance typeParameterVariance, Variance projectionKind) {
        if (typeParameterVariance == Variance.INVARIANT) return projectionKind;
        if (projectionKind == Variance.INVARIANT) return typeParameterVariance;
        if (typeParameterVariance == projectionKind) return projectionKind;
        return Variance.IN_VARIANCE;
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
