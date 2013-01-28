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

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Set;

public class TypeConstraintsImpl implements TypeConstraints {
    private final Variance varianceOfPosition;
    private final Set<JetType> upperBounds = Sets.newLinkedHashSet();
    private final Set<JetType> lowerBounds = Sets.newLinkedHashSet();
    private final Set<JetType> exactBounds = Sets.newLinkedHashSet();

    public TypeConstraintsImpl(Variance varianceOfPosition) {
        this.varianceOfPosition = varianceOfPosition;
    }

    @NotNull
    @Override
    public Variance getVarianceOfPosition() {
        return varianceOfPosition;
    }

    public void addBound(@NotNull ConstraintKind constraintKind, @NotNull JetType type) {
        switch (constraintKind) {
            case SUB_TYPE:
                lowerBounds.add(type);
                break;
            case SUPER_TYPE:
                upperBounds.add(type);
                break;
            case EQUAL:
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
        for (JetType upperBound : upperBounds) {
            typeConstraints.upperBounds.add(upperBound);
        }
        for (JetType lowerBound : lowerBounds) {
            typeConstraints.lowerBounds.add(lowerBound);
        }
        for (JetType exactBound : exactBounds) {
            typeConstraints.exactBounds.add(exactBound);
        }
        return typeConstraints;
    }

    public static enum ConstraintKind {
        SUPER_TYPE, SUB_TYPE, EQUAL
    }
}
