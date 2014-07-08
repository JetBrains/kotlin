/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.optimization.boxing;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RedundantBoxedValuesCollection implements Iterable<BoxedBasicValue> {
    private final Set<BoxedBasicValue> safeToDeleteValues = new HashSet<BoxedBasicValue>();

    public void add(@NotNull BoxedBasicValue value) {
        safeToDeleteValues.add(value);
    }

    public void remove(@NotNull BoxedBasicValue value) {
        if (safeToDeleteValues.contains(value)) {
            safeToDeleteValues.remove(value);
            value.markAsUnsafeToRemove();

            for (BoxedBasicValue mergedValue : value.getMergedWith()) {
                remove(mergedValue);
            }
        }
    }

    public void merge(@NotNull BoxedBasicValue v, @NotNull BoxedBasicValue w) {
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
    public Iterator<BoxedBasicValue> iterator() {
        return safeToDeleteValues.iterator();
    }
}
