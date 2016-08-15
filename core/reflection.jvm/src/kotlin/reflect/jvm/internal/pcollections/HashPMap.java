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

package kotlin.reflect.jvm.internal.pcollections;

import org.jetbrains.annotations.NotNull;

/**
 * A persistent map from non-null keys to non-null values.
 * @suppress
 */
public final class HashPMap<K, V> {
    private static final HashPMap<Object, Object> EMPTY = new HashPMap<Object, Object>(IntTreePMap.<ConsPStack<MapEntry<Object, Object>>>empty(), 0);

    @SuppressWarnings("unchecked")
    @NotNull
    public static <K, V> HashPMap<K, V> empty() {
        return (HashPMap<K, V>) EMPTY;
    }

    private final IntTreePMap<ConsPStack<MapEntry<K, V>>> intMap;
    private final int size;

    private HashPMap(IntTreePMap<ConsPStack<MapEntry<K, V>>> intMap, int size) {
        this.intMap = intMap;
        this.size = size;
    }

    public int size() {
        return size;
    }

    public boolean containsKey(Object key) {
        return keyIndexIn(getEntries(key.hashCode()), key) != -1;
    }

    public V get(Object key) {
        ConsPStack<MapEntry<K, V>> entries = getEntries(key.hashCode());
        while (entries != null && entries.size() > 0) {
            MapEntry<K, V> entry = entries.first;
            if (entry.key.equals(key))
                return entry.value;
            entries = entries.rest;
        }
        return null;
    }

    @NotNull
    public HashPMap<K, V> plus(K key, V value) {
        ConsPStack<MapEntry<K, V>> entries = getEntries(key.hashCode());
        int size0 = entries.size();
        int i = keyIndexIn(entries, key);
        if (i != -1) entries = entries.minus(i);
        entries = entries.plus(new MapEntry<K, V>(key, value));
        return new HashPMap<K, V>(intMap.plus(key.hashCode(), entries), size - size0 + entries.size());
    }

    @NotNull
    public HashPMap<K, V> minus(Object key) {
        ConsPStack<MapEntry<K, V>> entries = getEntries(key.hashCode());
        int i = keyIndexIn(entries, key);
        if (i == -1) // key not in this
            return this;
        entries = entries.minus(i);
        if (entries.size() == 0) // get rid of the entire hash entry
            return new HashPMap<K, V>(intMap.minus(key.hashCode()), size - 1);
        // otherwise replace hash entry with new smaller one:
        return new HashPMap<K, V>(intMap.plus(key.hashCode(), entries), size - 1);
    }

    private ConsPStack<MapEntry<K, V>> getEntries(int hash) {
        ConsPStack<MapEntry<K, V>> entries = intMap.get(hash);
        if (entries == null) return ConsPStack.empty();
        return entries;
    }

    private static <K, V> int keyIndexIn(ConsPStack<MapEntry<K, V>> entries, Object key) {
        int i = 0;
        while (entries != null && entries.size() > 0) {
            MapEntry<K, V> entry = entries.first;
            if (entry.key.equals(key))
                return i;
            entries = entries.rest;
            i++;
        }
        return -1;
    }
}
