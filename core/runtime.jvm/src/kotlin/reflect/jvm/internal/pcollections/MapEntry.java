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

package kotlin.reflect.jvm.internal.pcollections;

final class MapEntry<K, V> implements java.io.Serializable {
    private static final long serialVersionUID = 7138329143949025153L;

    private final K key;
    private final V value;

    public MapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapEntry)) return false;
        MapEntry<?, ?> e = (MapEntry<?, ?>) o;
        return (key == null ? e.key == null : key.equals(e.key)) &&
                (value == null ? e.value == null : value.equals(e.value));
    }

    @Override
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) ^
                (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
