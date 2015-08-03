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

package org.jetbrains.kotlin.serialization;

import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Interner<T> {
    private final Interner<T> parent;
    private final int firstIndex;
    private final Map<T, Integer> interned = new HashMap<T, Integer>();

    public Interner(Interner<T> parent) {
        this.parent = parent;
        this.firstIndex = parent != null ? parent.interned.size() + parent.firstIndex : 0;
    }

    public Interner() {
        this(null);
    }

    @Nullable
    private Integer find(@NotNull T obj) {
        assert parent == null || parent.interned.size() + parent.firstIndex == firstIndex :
                "Parent changed in parallel with child: indexes will be wrong";
        if (parent != null) {
            Integer index = parent.find(obj);
            if (index != null) return index;
        }
        return interned.get(obj);
    }

    public int intern(@NotNull T obj) {
        Integer index = find(obj);
        if (index != null) return index;

        index = firstIndex + interned.size();
        interned.put(obj, index);
        return index;
    }

    @NotNull
    public List<T> getAllInternedObjects() {
        return KotlinPackage.toSortedListBy(interned.keySet(), new Function1<T, Integer>() {
            @Override
            public Integer invoke(T key) {
                return interned.get(key);
            }
        });
    }
}
