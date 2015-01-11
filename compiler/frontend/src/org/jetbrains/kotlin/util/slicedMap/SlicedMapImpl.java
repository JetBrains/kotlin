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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class SlicedMapImpl implements MutableSlicedMap {

    public static SlicedMapImpl create() {
        return new SlicedMapImpl(Maps.<SlicedMapKey<?, ?>, Object>newLinkedHashMap());
    }

    public static SlicedMapImpl create(Map<SlicedMapKey<?, ?>, Object> map) {
        return new SlicedMapImpl(map);
    }

    public static SlicedMapImpl create(MapSupplier mapSupplier) {
        return new SlicedMapImpl(mapSupplier.<SlicedMapKey<?, ?>, Object>get());
    }
    
    private final Map<SlicedMapKey<?, ?>, Object> map;
    private final Multimap<WritableSlice<?, ?>, Object> collectiveSliceKeys = ArrayListMultimap.create();

    protected SlicedMapImpl(Map<SlicedMapKey<?, ?>, Object> map) {
        this.map = map;
    }

    @Override
    public <K, V> void put(WritableSlice<K, V> slice, K key, V value) {
        if (!slice.check(key, value)) {
            return;
        }

        SlicedMapKey<K, V> slicedMapKey = slice.makeKey(key);
        RewritePolicy rewritePolicy = slice.getRewritePolicy();
        if (rewritePolicy.rewriteProcessingNeeded(key)) {
            if (map.containsKey(slicedMapKey)) {
                //noinspection unchecked
                if (!rewritePolicy.processRewrite(slice, key, (V) map.get(slicedMapKey), value)) {
                    return;
                }
            }
        }

        if (slice.isCollective()) {
            collectiveSliceKeys.put(slice, key);
        }

        map.put(slicedMapKey, value);
        slice.afterPut(this, key, value);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        SlicedMapKey<K, V> slicedMapKey = slice.makeKey(key);
        //noinspection unchecked
        V value = (V) map.get(slicedMapKey);
        return slice.computeValue(this, key, value, value == null && !map.containsKey(slicedMapKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        assert slice.isCollective() : "Keys are not collected for slice " + slice;
        return (Collection<K>) collectiveSliceKeys.get(slice);
    }

    @Override
    public <K, V> V remove(RemovableSlice<K, V> slice, K key) {
        //noinspection unchecked
        return (V) map.remove(slice.makeKey(key));
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<SlicedMapKey<?, ?>, ?>> iterator() {
        //noinspection unchecked
        return (Iterator) map.entrySet().iterator();
    }

    @NotNull
    @Override
    public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        for (Map.Entry<SlicedMapKey<?, ?>, ?> entry : map.entrySet()) {
            if (entry.getKey().getSlice() == slice) {
                builder.put((K) entry.getKey().getKey(), (V) entry.getValue());
            }
        }
        return builder.build();
    }
}
