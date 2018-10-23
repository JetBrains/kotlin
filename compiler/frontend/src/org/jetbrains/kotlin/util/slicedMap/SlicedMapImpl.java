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
import com.google.common.collect.Multimap;
import com.intellij.openapi.util.Key;
import com.intellij.util.keyFMap.KeyFMap;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SlicedMapImpl implements MutableSlicedMap {

    private final boolean alwaysAllowRewrite;
    @Nullable
    private Map<Object, KeyFMap> map = null;
    private Multimap<WritableSlice<?, ?>, Object> collectiveSliceKeys = null;

    public SlicedMapImpl(boolean alwaysAllowRewrite) {
        this.alwaysAllowRewrite = alwaysAllowRewrite;
    }

    @Override
    public <K, V> void put(WritableSlice<K, V> slice, K key, V value) {
        if (!slice.check(key, value)) {
            return;
        }

        if (map == null) {
            map = new OpenAddressLinearProbingHashTable<>();
        }

        KeyFMap holder = map.get(key);
        if (holder == null) {
            holder = KeyFMap.EMPTY_MAP;
        }

        Key<V> sliceKey = slice.getKey();

        RewritePolicy rewritePolicy = slice.getRewritePolicy();
        if (!alwaysAllowRewrite && rewritePolicy.rewriteProcessingNeeded(key)) {
            V oldValue = holder.get(sliceKey);
            if (oldValue != null) {
                //noinspection unchecked
                if (!rewritePolicy.processRewrite(slice, key, oldValue, value)) {
                    return;
                }
            }
        }

        if (slice.isCollective()) {
            if (collectiveSliceKeys == null) {
                collectiveSliceKeys = ArrayListMultimap.create();
            }

            collectiveSliceKeys.put(slice, key);
        }

        map.put(key, holder.plus(sliceKey, value));
        slice.afterPut(this, key, value);
    }

    @Override
    public void clear() {
        map = null;
        collectiveSliceKeys = null;
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        KeyFMap holder = map != null ? map.get(key) : null;

        V value = holder == null ? null : holder.get(slice.getKey());

        return slice.computeValue(this, key, value, value == null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        assert slice.isCollective() : "Keys are not collected for slice " + slice;

        if (collectiveSliceKeys == null) return Collections.emptyList();
        return (Collection<K>) collectiveSliceKeys.get(slice);
    }

    @Override
    public void forEach(@NotNull Function3<WritableSlice, Object, Object, Void> f) {
        if (map == null) return;
        map.forEach((key, holder) -> {
            if (holder == null) return;

            for (Key<?> sliceKey : holder.getKeys()) {
                Object value = holder.get(sliceKey);

                f.invoke(((AbstractWritableSlice) sliceKey).getSlice(), key, value);
            }
        });
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
        if (map == null) return ImmutableMap.of();

        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

        map.forEach((key, holder) -> {
            V value = holder.get(slice.getKey());

            if (value != null) {
                builder.put((K) key, value);
            }
        });

        return builder.build();
    }
}
