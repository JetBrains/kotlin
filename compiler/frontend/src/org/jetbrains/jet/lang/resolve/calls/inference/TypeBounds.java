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

import com.google.common.collect.Sets;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Set;

/**
 * @author svtk
 */
public class TypeBounds {
    private final Variance variance;
    private final Set<JetType> upperBounds = Sets.newLinkedHashSet();
    private final Set<JetType> lowerBounds = Sets.newLinkedHashSet();
    private final Set<JetType> exactValues = Sets.newLinkedHashSet();

    public TypeBounds(Variance variance) {
        this.variance = variance;
    }

    public Variance getVariance() {
        return variance;
    }

    public void addUpperBound(JetType upperBound) {
        upperBounds.add(upperBound);
    }

    public void addLowerBound(JetType lowerBound) {
        lowerBounds.add(lowerBound);
    }

    public Set<JetType> getUpperBounds() {
        return upperBounds;
    }

    public Set<JetType> getLowerBounds() {
        return lowerBounds;
    }

    public void setExactValue(JetType exactType) {
        exactValues.add(exactType);
    }

    public Set<JetType> getExactValues() {
        return exactValues;
    }

    public boolean isEmpty() {
        return lowerBounds.isEmpty() && upperBounds.isEmpty() && exactValues.isEmpty();
    }

    @Override
    public String toString() {
        return "TypeBounds{" +
               "upperBounds=" + upperBounds +
               ", lowerBounds=" + lowerBounds +
               ", exactValues=" + exactValues +
               '}';
    }
}
