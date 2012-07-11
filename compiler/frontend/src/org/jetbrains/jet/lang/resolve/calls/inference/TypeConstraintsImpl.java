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

package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.CommonSupertypes;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collections;
import java.util.Set;

/**
 * @author svtk
 */
public class TypeConstraintsImpl implements TypeConstraints {
    private static final TypeConstraintsData EMPTY_CONSTRAINTS_DATA = new TypeConstraintsData(null, null, Collections.<JetType>emptySet());
    private static class TypeConstraintsData {
        private final JetType lowerConstraint;
        private final JetType upperConstraint;
        private final Set<JetType> conflicts;

        private TypeConstraintsData(JetType lowerConstraint, JetType upperConstraint, Set<JetType> conflicts) {
            this.lowerConstraint = lowerConstraint;
            this.upperConstraint = upperConstraint;
            this.conflicts = conflicts;
        }

        public JetType getLowerConstraint() {
            return lowerConstraint;
        }

        public JetType getUpperConstraint() {
            return upperConstraint;
        }

        public Set<JetType> getConflicts() {
            return conflicts;
        }
    }

    private final Variance variance;
    private TypeConstraintsData typeConstraintsData = EMPTY_CONSTRAINTS_DATA;

    private final Set<JetType> upperConstraints = Sets.newLinkedHashSet();
    private final Set<JetType> lowerConstraints = Sets.newLinkedHashSet();
    private final Set<JetType> equalConstraints = Sets.newLinkedHashSet();

    private boolean changed = false;

    public TypeConstraintsImpl(Variance variance) {
        this.variance = variance;
    }

    @NotNull
    @Override
    public Variance getVariance() {
        return variance;
    }

    public void addUpperConstraint(@NotNull JetType constraint) {
        upperConstraints.add(constraint);
        changed = true;
    }

    public void addLowerConstraint(@NotNull JetType constraint) {
        lowerConstraints.add(constraint);
        changed = true;
    }

    public void addEqualConstraint(@NotNull JetType constraint) {
        equalConstraints.add(constraint);
        changed = true;
    }

    private void updateResult() {
        if (!changed) {
            return;
        }
        JetType lowerConstraint = null;
        JetType upperConstraint = null;
        Set<JetType> conflicts = Sets.newLinkedHashSet();
        if (!lowerConstraints.isEmpty()) {
            lowerConstraint = CommonSupertypes.commonSupertype(lowerConstraints);
        }
        if (!upperConstraints.isEmpty()) {
            //todo
            upperConstraint = upperConstraints.iterator().next();
        }

        if (lowerConstraint != null && upperConstraint != null && !JetTypeChecker.INSTANCE.isSubtypeOf(lowerConstraint, upperConstraint)) {
            conflicts.add(lowerConstraint);
            conflicts.add(upperConstraint);
        }

        if (equalConstraints.size() > 1) {
            conflicts.addAll(equalConstraints);
        }
        else if (equalConstraints.size() == 1) {
            JetType value = equalConstraints.iterator().next();
            if (lowerConstraint != null && !JetTypeChecker.INSTANCE.isSubtypeOf(lowerConstraint, value)) {
                conflicts.add(lowerConstraint);
                conflicts.add(value);
            }
            if (upperConstraint != null && !JetTypeChecker.INSTANCE.isSubtypeOf(value, upperConstraint)) {
                conflicts.add(upperConstraint);
                conflicts.add(value);
            }
            lowerConstraint = value;
            upperConstraint = value;
        }
        typeConstraintsData = new TypeConstraintsData(lowerConstraint, upperConstraint, conflicts);
    }

    @Override
    public boolean isSuccessful() {
        updateResult();
        return typeConstraintsData.getConflicts().isEmpty();
    }

    @Nullable
    @Override
    public JetType getLowerConstraint() {
        updateResult();
        return typeConstraintsData.getLowerConstraint();
    }

    @Nullable
    @Override
    public JetType getUpperConstraint() {
        updateResult();
        return typeConstraintsData.getUpperConstraint();
    }

    @NotNull
    @Override
    public Set<JetType> getConflicts() {
        updateResult();
        return typeConstraintsData.getConflicts();
    }

    @Override
    public boolean isEmpty() {
        updateResult();
        return typeConstraintsData.getLowerConstraint() == null && typeConstraintsData.getUpperConstraint() == null && typeConstraintsData.getConflicts().isEmpty();
    }

    @Override
    public String toString() {
        return "TypeConstraints{" +
               "upper=" + getUpperConstraint() +
               ", lower=" + getLowerConstraint() +
               ", conflicts=" + getConflicts() +
               '}';
    }
}
