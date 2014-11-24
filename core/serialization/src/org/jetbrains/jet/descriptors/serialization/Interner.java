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

package org.jetbrains.jet.descriptors.serialization;

import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Interner<T> {
    private final Interner<T> parent;
    private final int firstIndex;
    private final TObjectIntHashMap<T> interned;
    private final List<T> all = new ArrayList<T>();

    public Interner(Interner<T> parent, @NotNull TObjectHashingStrategy<T> hashing) {
        this.parent = parent;
        this.firstIndex = parent != null ? parent.all.size() + parent.firstIndex : 0;
        this.interned = new TObjectIntHashMap<T>(hashing);
    }

    public Interner(@NotNull TObjectHashingStrategy<T> hashing) {
        this(null, hashing);
    }

    @SuppressWarnings("unchecked")
    public Interner(@Nullable Interner<T> parent) {
        this(parent, TObjectHashingStrategy.CANONICAL);
    }

    public Interner() {
        this((Interner<T>) null);
    }

    private int find(@NotNull T obj) {
        assert parent == null || parent.all.size() + parent.firstIndex == firstIndex :
                "Parent changed in parallel with child: indexes will be wrong";
        if (parent != null) {
            int index = parent.find(obj);
            if (index >= 0) return index;
        }
        if (interned.contains(obj)) {
            return interned.get(obj);
        }
        return -1;
    }

    public int intern(@NotNull T obj) {
        int index = find(obj);
        if (index >= 0) return index;

        index = firstIndex + interned.size();
        interned.put(obj, index);
        all.add(obj);
        return index;
    }

    @NotNull
    public List<T> getAllInternedObjects() {
        return all;
    }
}
