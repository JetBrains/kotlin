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

package org.jetbrains.kotlin.util.slicedMap;

import org.jetbrains.annotations.NotNull;

public final class SlicedMapKey<K, V> {

    private final WritableSlice<K, V> slice;
    private final K key;

    public SlicedMapKey(@NotNull WritableSlice<K, V> slice, K key) {
        this.slice = slice;
        this.key = key;
    }

    public WritableSlice<K, V> getSlice() {
        return slice;
    }

    public K getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlicedMapKey)) return false;

        SlicedMapKey that = (SlicedMapKey) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (!slice.equals(that.slice)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = slice.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return slice + " -> " + key;
    }
}
