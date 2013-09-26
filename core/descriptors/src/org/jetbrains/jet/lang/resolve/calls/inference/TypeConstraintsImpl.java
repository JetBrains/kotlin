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
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TypeConstraintsImpl implements TypeConstraints {
    public static enum BoundKind {
        LOWER_BOUND, UPPER_BOUND, EXACT_BOUND
    }

    public static class Constraint {
        public final JetType type;
        public final BoundKind boundKind;
        public final ConstraintPosition constraintPosition;

        public Constraint(@NotNull JetType type, @NotNull BoundKind boundKind, @NotNull ConstraintPosition constraintPosition) {
            this.type = type;
            this.boundKind = boundKind;
            this.constraintPosition = constraintPosition;
        }
    }

    private final TypeParameterDescriptor typeVariable;
    private final Variance varianceOfPosition;
    private final Set<Constraint> constraints = Sets.newLinkedHashSet();

    private Collection<JetType> resultValues;

    public TypeConstraintsImpl(
            @NotNull TypeParameterDescriptor typeVariable,
            @NotNull Variance varianceOfPosition
    ) {
        this.typeVariable = typeVariable;
        this.varianceOfPosition = varianceOfPosition;
    }

    @NotNull
    @Override
    public Variance getVarianceOfPosition() {
        return varianceOfPosition;
    }

    public void addConstraint(@NotNull BoundKind boundKind, @NotNull JetType type, @NotNull ConstraintPosition constraintPosition) {
        resultValues = null;
        constraints.add(new Constraint(type, boundKind, constraintPosition));
    }

    @Override
    public boolean isEmpty() {
        return getValues().isEmpty();
    }

    @NotNull
    public Collection<Constraint> getConstraints() {
        return constraints;
    }

    @NotNull
    private static Set<JetType> filterBounds(
            @NotNull Collection<Constraint> constraints,
            @NotNull BoundKind boundKind
    ) {
        return filterBounds(constraints, boundKind, null);
    }

    @NotNull
    private static Set<JetType> filterBounds(
            @NotNull Collection<Constraint> constraints,
            @NotNull BoundKind boundKind,
            @Nullable Collection<JetType> errorValues
    ) {
        Set<JetType> result = Sets.newLinkedHashSet();
        for (Constraint constraint : constraints) {
            if (constraint.boundKind == boundKind) {
                if (!ErrorUtils.containsErrorType(constraint.type)) {
                    result.add(constraint.type);
                }
                else if (errorValues != null) {
                    errorValues.add(constraint.type);
                }
            }
        }
        return result;
    }

    /*package*/ TypeConstraintsImpl copy() {
        TypeConstraintsImpl typeConstraints = new TypeConstraintsImpl(typeVariable, varianceOfPosition);
        typeConstraints.constraints.addAll(constraints);
        typeConstraints.resultValues = resultValues;
        return typeConstraints;
    }

    @NotNull
    public TypeConstraintsImpl filter(@NotNull final Condition<ConstraintPosition> condition) {
        TypeConstraintsImpl result = new TypeConstraintsImpl(typeVariable, varianceOfPosition);
        result.constraints.addAll(ContainerUtil.filter(constraints, new Condition<Constraint>() {
            @Override
            public boolean value(Constraint constraint) {
                return condition.value(constraint.constraintPosition);
            }
        }));
        return result;
    }

    @Nullable
    @Override
    public JetType getValue() {
        Collection<JetType> values = getValues();
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return null;
    }

    @NotNull
    @Override
    public Collection<JetType> getValues() {
        if (resultValues == null) {
            resultValues = computeValues(constraints);
        }
        return resultValues;
    }

    @NotNull
    private static Collection<JetType> computeValues(@NotNull Collection<Constraint> constraints) {
        Set<JetType> values = Sets.newLinkedHashSet();
        if (constraints.isEmpty()) {
            return Collections.emptyList();
        }

        Set<JetType> exactBounds = filterBounds(constraints, BoundKind.EXACT_BOUND, values);
        if (exactBounds.size() == 1) {
            JetType exactBound = exactBounds.iterator().next();
            if (trySuggestion(exactBound, constraints)) {
                return Collections.singleton(exactBound);
            }
        }
        values.addAll(exactBounds);

        Pair<Collection<JetType>, Collection<JetType>> pair =
                TypeUtils.filterNumberTypes(filterBounds(constraints, BoundKind.LOWER_BOUND, values));
        Collection<JetType> generalLowerBounds = pair.getFirst();
        Collection<JetType> numberLowerBounds = pair.getSecond();

        JetType superTypeOfLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(generalLowerBounds);
        if (trySuggestion(superTypeOfLowerBounds, constraints)) {
            return Collections.singleton(superTypeOfLowerBounds);
        }
        ContainerUtil.addIfNotNull(superTypeOfLowerBounds, values);

        Set<JetType> upperBounds = filterBounds(constraints, BoundKind.UPPER_BOUND, values);
        for (JetType upperBound : upperBounds) {
            if (trySuggestion(upperBound, constraints)) {
                return Collections.singleton(upperBound);
            }
        }
        //todo
        //fun <T> foo(t: T, consumer: Consumer<T>): T
        //foo(1, c: Consumer<Any>) - infer Int, not Any here

        values.addAll(filterBounds(constraints, BoundKind.UPPER_BOUND));

        JetType superTypeOfNumberLowerBounds = TypeUtils.commonSupertypeForNumberTypes(numberLowerBounds);
        if (trySuggestion(superTypeOfNumberLowerBounds, constraints)) {
            return Collections.singleton(superTypeOfNumberLowerBounds);
        }
        ContainerUtil.addIfNotNull(superTypeOfNumberLowerBounds, values);

        if (superTypeOfLowerBounds != null && superTypeOfNumberLowerBounds != null) {
            JetType superTypeOfAllLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(
                    Lists.newArrayList(superTypeOfLowerBounds, superTypeOfNumberLowerBounds));
            if (trySuggestion(superTypeOfAllLowerBounds, constraints)) {
                return Collections.singleton(superTypeOfAllLowerBounds);
            }
        }
        return values;
    }

    private static boolean trySuggestion(
            @Nullable JetType suggestion,
            @NotNull Collection<Constraint> constraints
    ) {
        if (suggestion == null) return false;
        if (!suggestion.getConstructor().isDenotable()) return false;
        if (filterBounds(constraints, BoundKind.EXACT_BOUND).size() > 1) return false;

        for (Constraint constraint : constraints) {
            switch (constraint.boundKind) {
                case LOWER_BOUND:
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(constraint.type, suggestion)) {
                        return false;
                    }
                    break;

                case UPPER_BOUND:
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(suggestion, constraint.type)) {
                        return false;
                    }
                    break;

                case EXACT_BOUND:
                    if (!JetTypeChecker.INSTANCE.equalTypes(constraint.type, suggestion)) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }
}
