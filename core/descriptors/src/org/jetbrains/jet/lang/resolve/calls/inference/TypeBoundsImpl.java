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

public class TypeBoundsImpl implements TypeBounds {
    public static enum BoundKind {
        LOWER_BOUND, UPPER_BOUND, EXACT_BOUND
    }

    public static class Bound {
        public final JetType type;
        public final BoundKind kind;
        public final ConstraintPosition position;

        public Bound(@NotNull JetType type, @NotNull BoundKind kind, @NotNull ConstraintPosition position) {
            this.type = type;
            this.kind = kind;
            this.position = position;
        }
    }

    private final TypeParameterDescriptor typeVariable;
    private final Variance varianceOfPosition;
    private final Set<Bound> bounds = Sets.newLinkedHashSet();

    private Collection<JetType> resultValues;

    public TypeBoundsImpl(
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

    public void addBound(@NotNull BoundKind kind, @NotNull JetType type, @NotNull ConstraintPosition position) {
        resultValues = null;
        bounds.add(new Bound(type, kind, position));
    }

    @Override
    public boolean isEmpty() {
        return getValues().isEmpty();
    }

    @NotNull
    public Collection<Bound> getBounds() {
        return bounds;
    }

    @NotNull
    private static Set<JetType> filterBounds(
            @NotNull Collection<Bound> bounds,
            @NotNull BoundKind kind
    ) {
        return filterBounds(bounds, kind, null);
    }

    @NotNull
    private static Set<JetType> filterBounds(
            @NotNull Collection<Bound> bounds,
            @NotNull BoundKind kind,
            @Nullable Collection<JetType> errorValues
    ) {
        Set<JetType> result = Sets.newLinkedHashSet();
        for (Bound bound : bounds) {
            if (bound.kind == kind) {
                if (!ErrorUtils.containsErrorType(bound.type)) {
                    result.add(bound.type);
                }
                else if (errorValues != null) {
                    errorValues.add(bound.type);
                }
            }
        }
        return result;
    }

    /*package*/ TypeBoundsImpl copy() {
        TypeBoundsImpl typeBounds = new TypeBoundsImpl(typeVariable, varianceOfPosition);
        typeBounds.bounds.addAll(bounds);
        typeBounds.resultValues = resultValues;
        return typeBounds;
    }

    @NotNull
    public TypeBoundsImpl filter(@NotNull final Condition<ConstraintPosition> condition) {
        TypeBoundsImpl result = new TypeBoundsImpl(typeVariable, varianceOfPosition);
        result.bounds.addAll(ContainerUtil.filter(bounds, new Condition<Bound>() {
            @Override
            public boolean value(Bound bound) {
                return condition.value(bound.position);
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
            resultValues = computeValues();
        }
        return resultValues;
    }

    @NotNull
    private Collection<JetType> computeValues() {
        Set<JetType> values = Sets.newLinkedHashSet();
        if (bounds.isEmpty()) {
            return Collections.emptyList();
        }
        boolean hasStrongBound = ContainerUtil.exists(bounds, new Condition<Bound>() {
            @Override
            public boolean value(Bound bound) {
                return bound.position.isStrong();
            }
        });
        if (!hasStrongBound) {
            return Collections.emptyList();
        }

        Set<JetType> exactBounds = filterBounds(bounds, BoundKind.EXACT_BOUND, values);
        if (exactBounds.size() == 1) {
            JetType exactBound = exactBounds.iterator().next();
            if (tryPossibleAnswer(exactBound)) {
                return Collections.singleton(exactBound);
            }
        }
        values.addAll(exactBounds);

        Pair<Collection<JetType>, Collection<JetType>> pair =
                TypeUtils.filterNumberTypes(filterBounds(bounds, BoundKind.LOWER_BOUND, values));
        Collection<JetType> generalLowerBounds = pair.getFirst();
        Collection<JetType> numberLowerBounds = pair.getSecond();

        JetType superTypeOfLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(generalLowerBounds);
        if (tryPossibleAnswer(superTypeOfLowerBounds)) {
            return Collections.singleton(superTypeOfLowerBounds);
        }
        ContainerUtil.addIfNotNull(superTypeOfLowerBounds, values);

        Set<JetType> upperBounds = filterBounds(bounds, BoundKind.UPPER_BOUND, values);
        JetType intersectionOfUpperBounds = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
        if (!upperBounds.isEmpty() && intersectionOfUpperBounds != null) {
            if (tryPossibleAnswer(intersectionOfUpperBounds)) {
                return Collections.singleton(intersectionOfUpperBounds);
            }
        }
        //todo
        //fun <T> foo(t: T, consumer: Consumer<T>): T
        //foo(1, c: Consumer<Any>) - infer Int, not Any here

        values.addAll(filterBounds(bounds, BoundKind.UPPER_BOUND));

        JetType superTypeOfNumberLowerBounds = TypeUtils.commonSupertypeForNumberTypes(numberLowerBounds);
        if (tryPossibleAnswer(superTypeOfNumberLowerBounds)) {
            return Collections.singleton(superTypeOfNumberLowerBounds);
        }
        ContainerUtil.addIfNotNull(superTypeOfNumberLowerBounds, values);

        if (superTypeOfLowerBounds != null && superTypeOfNumberLowerBounds != null) {
            JetType superTypeOfAllLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(
                    Lists.newArrayList(superTypeOfLowerBounds, superTypeOfNumberLowerBounds));
            if (tryPossibleAnswer(superTypeOfAllLowerBounds)) {
                return Collections.singleton(superTypeOfAllLowerBounds);
            }
        }
        return values;
    }

    private boolean tryPossibleAnswer(@Nullable JetType possibleAnswer) {
        if (possibleAnswer == null) return false;
        if (!possibleAnswer.getConstructor().isDenotable()) return false;

        for (Bound bound : bounds) {
            switch (bound.kind) {
                case LOWER_BOUND:
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(bound.type, possibleAnswer)) {
                        return false;
                    }
                    break;

                case UPPER_BOUND:
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(possibleAnswer, bound.type)) {
                        return false;
                    }
                    break;

                case EXACT_BOUND:
                    if (!JetTypeChecker.INSTANCE.equalTypes(bound.type, possibleAnswer)) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }
}
