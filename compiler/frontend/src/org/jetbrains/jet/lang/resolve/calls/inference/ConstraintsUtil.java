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

package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.constants.NumberValueTypeConstructor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public class ConstraintsUtil {

    @NotNull
    public static Set<JetType> getValues(@Nullable TypeConstraints typeConstraints) {
        Set<JetType> values = Sets.newLinkedHashSet();
        if (typeConstraints == null || typeConstraints.isEmpty()) {
            return values;
        }
        TypeConstraints typeConstraintsWithoutErrorTypes = filterNotContainingErrorType(typeConstraints, values);
        Collection<JetType> exactBounds = typeConstraintsWithoutErrorTypes.getExactBounds();
        if (exactBounds.size() == 1) {
            JetType exactBound = exactBounds.iterator().next();
            if (trySuggestion(exactBound, typeConstraints)) {
                return Collections.singleton(exactBound);
            }
        }
        values.addAll(exactBounds);

        Collection<JetType> lowerBounds = Sets.newHashSet();
        Collection<JetType> numberLowerBounds = Sets.newHashSet();
        for (JetType lowerBound : typeConstraintsWithoutErrorTypes.getLowerBounds()) {
            if (lowerBound.getConstructor() instanceof NumberValueTypeConstructor) {
                numberLowerBounds.add(lowerBound);
            }
            else {
                lowerBounds.add(lowerBound);
            }
        }
        JetType superTypeOfLowerBounds = commonSupertype(lowerBounds);
        if (trySuggestion(superTypeOfLowerBounds, typeConstraints)) {
            return Collections.singleton(superTypeOfLowerBounds);
        }
        addToValuesIfDifferent(superTypeOfLowerBounds, values);

        if (values.isEmpty()) {
            Collection<JetType> upperBounds = typeConstraintsWithoutErrorTypes.getUpperBounds();
            for (JetType upperBound : upperBounds) {
                if (trySuggestion(upperBound, typeConstraints)) {
                    return Collections.singleton(upperBound);
                }
            }
        }
        //todo
        //fun <T> foo(t: T, consumer: Consumer<T>): T
        //foo(1, c: Consumer<Any>) - infer Int, not Any here

        for (JetType upperBound : typeConstraintsWithoutErrorTypes.getUpperBounds()) {
            addToValuesIfDifferent(upperBound, values);
        }

        JetType superTypeOfNumberLowerBounds = commonSupertypeForNumberTypes(numberLowerBounds);
        if (trySuggestion(superTypeOfNumberLowerBounds, typeConstraints)) {
            return Collections.singleton(superTypeOfNumberLowerBounds);
        }
        addToValuesIfDifferent(superTypeOfNumberLowerBounds, values);

        if (superTypeOfLowerBounds != null && superTypeOfNumberLowerBounds != null) {
            JetType superTypeOfAllLowerBounds = commonSupertype(Lists.newArrayList(superTypeOfLowerBounds, superTypeOfNumberLowerBounds));
            if (trySuggestion(superTypeOfAllLowerBounds, typeConstraints)) {
                return Collections.singleton(superTypeOfAllLowerBounds);
            }
        }
        return values;
    }

    private static void addToValuesIfDifferent(@Nullable JetType type, @NotNull Set<JetType> values) {
        if (type == null) return;
        if (values.isEmpty()) {
            values.add(type);
            return;
        }
        for (JetType value : values) {
            if (!JetTypeChecker.INSTANCE.equalTypes(type, value)) {
                values.add(type);
                return;
            }
        }
    }

    @Nullable
    private static JetType commonSupertype(@NotNull Collection<JetType> lowerBounds) {
        if (lowerBounds.isEmpty()) return null;
        return CommonSupertypes.commonSupertype(lowerBounds);
    }

    @Nullable
    private static JetType commonSupertypeForNumberTypes(@NotNull Collection<JetType> numberLowerBounds) {
        if (numberLowerBounds.isEmpty()) return null;
        return TypeUtils.commonSupertypeForNumberTypes(numberLowerBounds);
    }

    private static boolean trySuggestion(
            @Nullable JetType suggestion,
            @NotNull TypeConstraints typeConstraints
    ) {
        if (suggestion == null) return false;
        if (typeConstraints.getExactBounds().size() > 1) return false;

        for (JetType exactBound : typeConstraints.getExactBounds()) {
            if (!JetTypeChecker.INSTANCE.equalTypes(exactBound, suggestion)) {
                return false;
            }
        }
        for (JetType lowerBound : typeConstraints.getLowerBounds()) {
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(lowerBound, suggestion)) {
                return false;
            }
        }
        for (JetType upperBound : typeConstraints.getUpperBounds()) {
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(suggestion, upperBound)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private static TypeConstraints filterNotContainingErrorType(
            @NotNull TypeConstraints typeConstraints,
            @NotNull Collection<JetType> values
    ) {
        TypeConstraintsImpl typeConstraintsWithoutErrorType = new TypeConstraintsImpl(typeConstraints.getVarianceOfPosition());
        Collection<Pair<TypeConstraintsImpl.BoundKind, JetType>> allBounds = ((TypeConstraintsImpl) typeConstraints).getAllBounds();
        for (Pair<TypeConstraintsImpl.BoundKind, JetType> pair : allBounds) {
            TypeConstraintsImpl.BoundKind boundKind = pair.getFirst();
            JetType type = pair.getSecond();
            if (ErrorUtils.containsErrorType(type)) {
                values.add(type);
            }
            else {
                typeConstraintsWithoutErrorType.addBound(boundKind, type);
            }
        }
        return typeConstraintsWithoutErrorType;
    }

    @Nullable
    public static JetType getValue(@Nullable TypeConstraints typeConstraints) {
        //todo all checks
        //todo variance dependance
        if (typeConstraints == null) {
            //todo assert typeConstraints != null;
            return null;
        }
        Set<JetType> values = getValues(typeConstraints);
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return null;
    }



    @Nullable
    public static TypeParameterDescriptor getFirstConflictingParameter(@NotNull ConstraintSystem constraintSystem) {
        for (TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables()) {
            TypeConstraints constraints = constraintSystem.getTypeConstraints(typeParameter);
            if (getValues(constraints).size() > 1) {
                return typeParameter;
            }
        }
        return null;
    }

    @NotNull
    public static Collection<TypeSubstitutor> getSubstitutorsForConflictingParameters(@NotNull ConstraintSystem constraintSystem) {
        TypeParameterDescriptor firstConflictingParameter = getFirstConflictingParameter(constraintSystem);
        if (firstConflictingParameter == null) return Collections.emptyList();

        Collection<JetType> conflictingTypes = getValues(constraintSystem.getTypeConstraints(firstConflictingParameter));

        ArrayList<Map<TypeConstructor, TypeProjection>> substitutionContexts = Lists.newArrayList();
        for (JetType type : conflictingTypes) {
            Map<TypeConstructor, TypeProjection> context = Maps.newLinkedHashMap();
            context.put(firstConflictingParameter.getTypeConstructor(), new TypeProjection(type));
            substitutionContexts.add(context);
        }

        for (TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables()) {
            if (typeParameter == firstConflictingParameter) continue;

            JetType safeType = getSafeValue(constraintSystem, typeParameter);
            for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
                TypeProjection typeProjection = new TypeProjection(safeType);
                context.put(typeParameter.getTypeConstructor(), typeProjection);
            }
        }
        Collection<TypeSubstitutor> typeSubstitutors = Lists.newArrayList();
        for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
            typeSubstitutors.add(TypeSubstitutor.create(context));
        }
        return typeSubstitutors;
    }

    @NotNull
    public static JetType getSafeValue(@NotNull ConstraintSystem constraintSystem, @NotNull TypeParameterDescriptor typeParameter) {
        TypeConstraints constraints = constraintSystem.getTypeConstraints(typeParameter);
        JetType type = getValue(constraints);
        if (type != null) {
            return type;
        }
        //todo may be error type
        return typeParameter.getUpperBoundsAsType();
    }

    public static boolean checkUpperBoundIsSatisfied(
            @NotNull ConstraintSystem constraintSystem,
            @NotNull TypeParameterDescriptor typeParameter,
            boolean substituteOtherTypeParametersInBound
    ) {
        TypeConstraints typeConstraints = constraintSystem.getTypeConstraints(typeParameter);
        assert typeConstraints != null;
        JetType type = getValue(typeConstraints);
        if (type == null) return true;
        for (JetType upperBound : typeParameter.getUpperBounds()) {
            if (!substituteOtherTypeParametersInBound && TypeUtils.dependsOnTypeParameters(upperBound, constraintSystem.getTypeVariables())) {
                continue;
            }
            JetType substitutedUpperBound = constraintSystem.getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT);

            assert substitutedUpperBound != null : "We wanted to substitute projections as a result for " + typeParameter;
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(type, substitutedUpperBound)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkBoundsAreSatisfied(
            @NotNull ConstraintSystem constraintSystem,
            boolean substituteOtherTypeParametersInBounds
    ) {
        for (TypeParameterDescriptor typeVariable : constraintSystem.getTypeVariables()) {
            if (!checkUpperBoundIsSatisfied(constraintSystem, typeVariable, substituteOtherTypeParametersInBounds)) {
                return false;
            }
        }
        return true;
    }
}
