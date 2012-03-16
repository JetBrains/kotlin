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

package org.jetbrains.jet.lang.types;

import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;

import java.util.*;

/**
 * @author abreslav
 */
public class TypeSubstitutor {

    private static final int MAX_RECURSION_DEPTH = 100;

    public static TypeSubstitutor makeConstantSubstitutor(Collection<TypeParameterDescriptor> typeParameterDescriptors, JetType type) {
        final Set<TypeConstructor> constructors = Sets.newHashSet();
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameterDescriptors) {
            constructors.add(typeParameterDescriptor.getTypeConstructor());
        }
        final TypeProjection projection = new TypeProjection(type);

        return TypeSubstitutor.create(new TypeSubstitutor.TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                if (constructors.contains(key)) {
                    return projection;
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public String toString() {
                return "TypeConstructor.makeConstantSubstitutor(" + constructors + " -> " + projection + ")";
            }
        });
    }

    public interface TypeSubstitution {
        TypeSubstitution EMPTY = new TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                return null;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public String toString() {
                return "Empty TypeSubstitution";
            }
        };
        
        @Nullable
        TypeProjection get(TypeConstructor key);
        boolean isEmpty();
    }

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

    public static final class SubstitutionException extends Exception {
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
        return create(TypeUtils.buildSubstitutionContext(context));
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
            return unsafeSubstitute(type, howThisTypeIsUsed);
        } catch (SubstitutionException e) {
            return ErrorUtils.createErrorType(e.getMessage());
        }
    }

    @Nullable
    public JetType substitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
        if (isEmpty()) {
            return type;
        }

        try {
            return unsafeSubstitute(type, howThisTypeIsUsed);
        } catch (SubstitutionException e) {
            return null;
        }
    }

    @NotNull
    private JetType unsafeSubstitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) throws SubstitutionException {
        if (ErrorUtils.isErrorType(type)) return type;

        TypeProjection value = getValueWithCorrectNullability(substitution, type);
        if (value != null) {
            TypeConstructor constructor = type.getConstructor();
            assert constructor.getDeclarationDescriptor() instanceof TypeParameterDescriptor;

            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) constructor.getDeclarationDescriptor();

            TypeProjection result = substitutionResult(typeParameterDescriptor, howThisTypeIsUsed, Variance.INVARIANT, value, 0);

            return TypeUtils.makeNullableIfNeeded(result.getType(), type.isNullable());
        }

        return specializeType(type, howThisTypeIsUsed, 0);
    }

    private TypeProjection getValueWithCorrectNullability(TypeSubstitution substitution, JetType type) {
        TypeProjection typeProjection = substitution.get(type.getConstructor());
        if (typeProjection == null) return null;

        return type.isNullable() ? makeNullableProjection(typeProjection) : typeProjection;
    }

    @NotNull
    private static TypeProjection makeNullableProjection(@NotNull TypeProjection value) {
        return new TypeProjection(value.getProjectionKind(), TypeUtils.makeNullable(value.getType()));
    }

    private JetType specializeType(JetType subjectType, Variance callSiteVariance, int recursionDepth) throws SubstitutionException {
        assertRecursionDepth(recursionDepth, subjectType, substitution);
        if (ErrorUtils.isErrorType(subjectType)) return subjectType;

        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        List<TypeProjection> arguments = subjectType.getArguments();
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            TypeProjection argument = arguments.get(i);
            TypeParameterDescriptor parameterDescriptor = subjectType.getConstructor().getParameters().get(i);
            newArguments.add(substituteInProjection(
                    substitution,
                    argument,
                    parameterDescriptor,
                    callSiteVariance, recursionDepth + 1));
        }
        return new JetTypeImpl(
                subjectType.getAnnotations(),
                subjectType.getConstructor(),
                subjectType.isNullable(),
                newArguments,
                new SubstitutingScope(subjectType.getMemberScope(), this));
    }

    @NotNull
    private TypeProjection substituteInProjection(
            @NotNull TypeSubstitution substitutionContext,
            @NotNull TypeProjection passedProjection,
            @NotNull TypeParameterDescriptor correspondingTypeParameter,
            @NotNull Variance contextCallSiteVariance,
            int recursionDepth) throws SubstitutionException {
        assertRecursionDepth(recursionDepth, correspondingTypeParameter, passedProjection, substitution);

        JetType typeToSubstituteIn = passedProjection.getType();
        if (ErrorUtils.isErrorType(typeToSubstituteIn)) return passedProjection;

        Variance passedProjectionKind = passedProjection.getProjectionKind();
        Variance parameterVariance = correspondingTypeParameter.getVariance();

        Variance effectiveProjectionKind = asymmetricOr(passedProjectionKind, parameterVariance);
        Variance effectiveContextVariance = contextCallSiteVariance.superpose(effectiveProjectionKind);

        TypeProjection projectionValue = getValueWithCorrectNullability(substitutionContext, typeToSubstituteIn);
        if (projectionValue != null) {
            assert typeToSubstituteIn.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;

            if (!allows(parameterVariance, passedProjectionKind)) {
                return TypeUtils.makeStarProjection(correspondingTypeParameter);
            }

            return substitutionResult(correspondingTypeParameter, effectiveContextVariance, passedProjectionKind, projectionValue, recursionDepth + 1);
        }
        return new TypeProjection(
                passedProjectionKind,
                specializeType(
                        typeToSubstituteIn,
                        effectiveContextVariance, recursionDepth + 1));
    }

    private TypeProjection substitutionResult(
            TypeParameterDescriptor correspondingTypeParameter,
            Variance effectiveContextVariance,
            Variance passedProjectionKind,
            TypeProjection value,
            int recursionDepth) throws SubstitutionException {
        assertRecursionDepth(recursionDepth, correspondingTypeParameter, value, substitution);

        Variance projectionKindValue = value.getProjectionKind();
        JetType typeValue = value.getType();
        Variance effectiveProjectionKindValue = asymmetricOr(passedProjectionKind, projectionKindValue);
        JetType effectiveTypeValue;
        switch (effectiveContextVariance) {
            case INVARIANT:
                effectiveProjectionKindValue = projectionKindValue;
                effectiveTypeValue = typeValue;
                break;
            case IN_VARIANCE:
                if (projectionKindValue == Variance.OUT_VARIANCE) {
                    throw new SubstitutionException(""); // TODO
//                    effectiveProjectionKindValue = Variance.INVARIANT;
//                    effectiveTypeValue = JetStandardClasses.getNothingType();
                }
                else {
                    effectiveTypeValue = typeValue;
                }
                break;
            case OUT_VARIANCE:
                if (projectionKindValue == Variance.IN_VARIANCE) {
                    effectiveProjectionKindValue = Variance.INVARIANT;
                    effectiveTypeValue = correspondingTypeParameter.getUpperBoundsAsType();
                }
                else {
                    effectiveTypeValue = typeValue;
                }
                break;
            default:
                throw new IllegalStateException(effectiveContextVariance.toString());
        }

//            if (!allows(effectiveContextVariance, projectionKindValue)) {
//                throw new SubstitutionException(""); // TODO : error message
//            }
//
        return new TypeProjection(effectiveProjectionKindValue, specializeType(effectiveTypeValue, effectiveContextVariance, recursionDepth + 1));
    }

    private static Variance asymmetricOr(Variance a, Variance b) {
        return a == Variance.INVARIANT ? b : a;
    }

    private static boolean allows(Variance declarationSiteVariance, Variance callSiteVariance) {
        switch (declarationSiteVariance) {
            case INVARIANT: return true;
            case IN_VARIANCE: return callSiteVariance != Variance.OUT_VARIANCE;
            case OUT_VARIANCE: return callSiteVariance != Variance.IN_VARIANCE;
        }
        throw new IllegalStateException(declarationSiteVariance.toString());
    }

    private static void assertRecursionDepth(int recursionDepth, TypeParameterDescriptor parameter, TypeProjection value, TypeSubstitution substitution) {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            throw new IllegalStateException("Recursion too deep. Most likely infinite loop while substituting " + safeToString(value) + " for " + safeToString(parameter) + "; substitution: " + safeToString(substitution));
        }
    }

    private static void assertRecursionDepth(int recursionDepth, JetType type, TypeSubstitution substitution) {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            throw new IllegalStateException("Recursion too deep. Most likely infinite loop while substituting " + safeToString(type) + "; substitution: " + safeToString(substitution));
        }
    }

    private static String safeToString(Object o) {
        try {
            return o.toString();
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            return "[Exception while computing toString(): " + e + "]";
        }
    }
}
