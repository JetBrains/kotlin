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
        this.firstIndex = parent == null ? 0 : parent.all.size();
        this.interned = new TObjectIntHashMap<T>(hashing);
    }

    public Interner(@NotNull TObjectHashingStrategy<T> hashing) {
        this(null, hashing);
    }

    public Interner(@Nullable Interner<T> parent) {
        //noinspection unchecked
        this(parent, TObjectHashingStrategy.CANONICAL);
    }

    public Interner() {
        //noinspection unchecked
        this((Interner) null);
    }

    public int intern(@NotNull T obj) {
        assert parent == null || parent.all.size() == firstIndex : "Parent changed in parallel with child: indexes will be wrong";
        if (interned.contains(obj)) {
            return interned.get(obj);
        }
        int index = firstIndex + interned.size();
        interned.put(obj, index);
        all.add(obj);
        return index;
    }

    public List<T> getAllInternedObjects() {
        return all;
    }
}
