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
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TypeConstraintsImpl implements TypeConstraints {
    private final Variance varianceOfPosition;
    private final Set<JetType> upperBounds = Sets.newLinkedHashSet();
    private final Set<JetType> lowerBounds = Sets.newLinkedHashSet();
    private final Set<JetType> exactBounds = Sets.newLinkedHashSet();

    private Collection<JetType> resultValues;

    public TypeConstraintsImpl(Variance varianceOfPosition) {
        this.varianceOfPosition = varianceOfPosition;
    }

    @NotNull
    @Override
    public Variance getVarianceOfPosition() {
        return varianceOfPosition;
    }

    public void addBound(@NotNull BoundKind boundKind, @NotNull JetType type) {
        resultValues = null;
        switch (boundKind) {
            case LOWER_BOUND:
                lowerBounds.add(type);
                break;
            case UPPER_BOUND:
                upperBounds.add(type);
                break;
            case EXACT_BOUND:
                exactBounds.add(type);
        }
    }

    @Override
    public boolean isEmpty() {
        return upperBounds.isEmpty() && lowerBounds.isEmpty() && exactBounds.isEmpty();
    }

    @NotNull
    @Override
    public Set<JetType> getLowerBounds() {
        return lowerBounds;
    }

    @NotNull
    @Override
    public Set<JetType> getUpperBounds() {
        return upperBounds;
    }

    @NotNull
    @Override
    public Set<JetType> getExactBounds() {
        return exactBounds;
    }

    /*package*/ TypeConstraintsImpl copy() {
        TypeConstraintsImpl typeConstraints = new TypeConstraintsImpl(varianceOfPosition);
        typeConstraints.upperBounds.addAll(upperBounds);
        typeConstraints.lowerBounds.addAll(lowerBounds);
        typeConstraints.exactBounds.addAll(exactBounds);
        typeConstraints.resultValues = resultValues;
        return typeConstraints;
    }

    public static enum BoundKind {
        LOWER_BOUND, UPPER_BOUND, EXACT_BOUND
    }

    private Collection<Pair<BoundKind, JetType>> getAllBounds() {
        Collection<Pair<BoundKind, JetType>> result = Lists.newArrayList();
        for (JetType exactBound : exactBounds) {
            result.add(Pair.create(BoundKind.EXACT_BOUND, exactBound));
        }
        for (JetType exactBound : upperBounds) {
            result.add(Pair.create(BoundKind.UPPER_BOUND, exactBound));
        }
        for (JetType exactBound : lowerBounds) {
            result.add(Pair.create(BoundKind.LOWER_BOUND, exactBound));
        }
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

    private Collection<JetType> computeValues() {
        Set<JetType> values = Sets.newLinkedHashSet();
        if (isEmpty()) {
            return values;
        }
        TypeConstraints withoutErrorTypes = filterNotContainingErrorType(values);
        Collection<JetType> exactBounds = withoutErrorTypes.getExactBounds();
        if (exactBounds.size() == 1) {
            JetType exactBound = exactBounds.iterator().next();
            if (trySuggestion(exactBound)) {
                return Collections.singleton(exactBound);
            }
        }
        values.addAll(exactBounds);

        Pair<Collection<JetType>, Collection<JetType>> pair =
                TypeUtils.filterNumberTypes(withoutErrorTypes.getLowerBounds());
        Collection<JetType> generalLowerBounds = pair.getFirst();
        Collection<JetType> numberLowerBounds = pair.getSecond();

        JetType superTypeOfLowerBounds = commonSupertype(generalLowerBounds);
        if (trySuggestion(superTypeOfLowerBounds)) {
            return Collections.singleton(superTypeOfLowerBounds);
        }
        ContainerUtil.addIfNotNull(superTypeOfLowerBounds, values);

        Collection<JetType> upperBounds = withoutErrorTypes.getUpperBounds();
        for (JetType upperBound : upperBounds) {
            if (trySuggestion(upperBound)) {
                return Collections.singleton(upperBound);
            }
        }
        //todo
        //fun <T> foo(t: T, consumer: Consumer<T>): T
        //foo(1, c: Consumer<Any>) - infer Int, not Any here

        values.addAll(withoutErrorTypes.getUpperBounds());

        JetType superTypeOfNumberLowerBounds = commonSupertypeForNumberTypes(numberLowerBounds);
        if (trySuggestion(superTypeOfNumberLowerBounds)) {
            return Collections.singleton(superTypeOfNumberLowerBounds);
        }
        ContainerUtil.addIfNotNull(superTypeOfNumberLowerBounds, values);

        if (superTypeOfLowerBounds != null && superTypeOfNumberLowerBounds != null) {
            JetType superTypeOfAllLowerBounds = commonSupertype(Lists.newArrayList(superTypeOfLowerBounds, superTypeOfNumberLowerBounds));
            if (trySuggestion(superTypeOfAllLowerBounds)) {
                return Collections.singleton(superTypeOfAllLowerBounds);
            }
        }
        return values;
    }

    private boolean trySuggestion(
            @Nullable JetType suggestion
    ) {
        if (suggestion == null) return false;
        if (!suggestion.getConstructor().isDenotable()) return false;
        if (getExactBounds().size() > 1) return false;

        for (JetType exactBound : getExactBounds()) {
            if (!JetTypeChecker.INSTANCE.equalTypes(exactBound, suggestion)) {
                return false;
            }
        }
        for (JetType lowerBound : getLowerBounds()) {
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(lowerBound, suggestion)) {
                return false;
            }
        }
        for (JetType upperBound : getUpperBounds()) {
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(suggestion, upperBound)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private TypeConstraints filterNotContainingErrorType(
            @NotNull Collection<JetType> values
    ) {
        TypeConstraintsImpl typeConstraintsWithoutErrorType = new TypeConstraintsImpl(getVarianceOfPosition());
        Collection<Pair<TypeConstraintsImpl.BoundKind, JetType>> allBounds = getAllBounds();
        for (Pair<TypeConstraintsImpl.BoundKind, JetType> pair : allBounds) {
            TypeConstraintsImpl.BoundKind boundKind = pair.getFirst();
            JetType type = pair.getSecond();
            if (ErrorUtils.containsErrorType(type)) {
                values.add(type);
            }
            else if (type != null) {
                typeConstraintsWithoutErrorType.addBound(boundKind, type);
            }
        }
        return typeConstraintsWithoutErrorType;
    }

    @Nullable
    private static JetType commonSupertype(@NotNull Collection<JetType> lowerBounds) {
        if (lowerBounds.isEmpty()) return null;
        if (lowerBounds.size() == 1) {
            JetType type = lowerBounds.iterator().next();
            if (type.getConstructor() instanceof IntersectionTypeConstructor) {
                return commonSupertype(type.getConstructor().getSupertypes());
            }
        }
        return CommonSupertypes.commonSupertype(lowerBounds);
    }

    @Nullable
    private static JetType commonSupertypeForNumberTypes(@NotNull Collection<JetType> numberLowerBounds) {
        if (numberLowerBounds.isEmpty()) return null;
        return TypeUtils.commonSupertypeForNumberTypes(numberLowerBounds);
    }
}
