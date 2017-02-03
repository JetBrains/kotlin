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

package org.jetbrains.kotlin.codegen.optimization.boxing;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RedundantBoxedValuesCollection implements Iterable<BoxedValueDescriptor> {
    private final Set<BoxedValueDescriptor> safeToDeleteValues = new HashSet<BoxedValueDescriptor>();

    public void add(@NotNull BoxedValueDescriptor descriptor) {
        safeToDeleteValues.add(descriptor);
    }

    public void remove(@NotNull BoxedValueDescriptor descriptor) {
        if (safeToDeleteValues.contains(descriptor)) {
            safeToDeleteValues.remove(descriptor);
            descriptor.markAsUnsafeToRemove();

            for (BoxedValueDescriptor mergedValueDescriptor : descriptor.getMergedWith()) {
                remove(mergedValueDescriptor);
            }
        }
    }

    public void merge(@NotNull BoxedValueDescriptor v, @NotNull BoxedValueDescriptor w) {
        v.addMergedWith(w);
        w.addMergedWith(v);

        if (v.isSafeToRemove() && !w.isSafeToRemove()) {
            remove(v);
        }

        if (!v.isSafeToRemove() && w.isSafeToRemove()) {
            remove(w);
        }
    }

    public boolean isEmpty() {
        return safeToDeleteValues.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<BoxedValueDescriptor> iterator() {
        return safeToDeleteValues.iterator();
    }
}
