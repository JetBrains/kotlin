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

package org.jetbrains.jet.util.slicedmap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class TrackingSlicedMap implements MutableSlicedMap {

    private final MutableSlicedMap delegate;
    private final Map<ReadOnlySlice<?, ?>, SliceWithStackTrace<?, ?>> sliceTranslationMap = Maps.newHashMap();

    public TrackingSlicedMap(@NotNull MutableSlicedMap delegate) {
        this.delegate = delegate;
    }

    private <K, V> SliceWithStackTrace<K, V> wrapSlice(ReadOnlySlice<K, V> slice) {
        SliceWithStackTrace<?, ?> translated = sliceTranslationMap.get(slice);
        if (translated == null) {
            translated = new SliceWithStackTrace<K, V>(slice);
            sliceTranslationMap.put(slice, translated);
        }
        //noinspection unchecked
        return (SliceWithStackTrace) translated;
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        return delegate.get(wrapSlice(slice), key).value;
    }

    @Override
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        return delegate.getKeys(wrapSlice(slice));
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<SlicedMapKey<?, ?>, ?>> iterator() {
        Map<SlicedMapKey<?, ?>, Object> map = Maps.newHashMap();
        for (Map.Entry<SlicedMapKey<?, ?>, ?> entry : delegate) {
            map.put(entry.getKey(), ((WithStackTrace<?>) entry.getValue()).value);
        }
        //noinspection unchecked
        return (Iterator) map.entrySet().iterator();
    }

    @Override
    public <K, V> void put(WritableSlice<K, V> slice, K key, V value) {
        delegate.put(wrapSlice(slice), key, new WithStackTrace<V>(value));
    }

    @Override
    public <K, V> V remove(RemovableSlice<K, V> slice, K key) {
        return delegate.remove(wrapSlice(slice), key).value;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    @NotNull
    @TestOnly
    public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
        return delegate.getSliceContents(slice);
    }

    private static class WithStackTrace<V> {
        private final V value;
        private final StackTraceElement[] stackTrace;

        private WithStackTrace(V value) {
            this.value = value;
            this.stackTrace = Thread.currentThread().getStackTrace();
        }

        private Appendable printStackTrace(Appendable appendable) {
            Printer s = new Printer(appendable);
            s.println(value);
            s.println("Written at ");
            StackTraceElement[] trace = stackTrace;
            for (StackTraceElement aTrace : trace) {
                s.println("\tat " + aTrace);
            }
            s.println("---------");
            return appendable;
        }

        @Override
        public String toString() {
            return printStackTrace(new StringBuilder()).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WithStackTrace other = (WithStackTrace) o;

            if (value != null ? !value.equals(other.value) : other.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    public class SliceWithStackTrace<K, V> implements RemovableSlice<K, WithStackTrace<V>> {

        private final ReadOnlySlice<K, V> delegate;

        private SliceWithStackTrace(@NotNull ReadOnlySlice<K, V> delegate) {
            this.delegate = delegate;
        }

        // Methods of ReadOnlySlice

        @Override
        public SlicedMapKey<K, WithStackTrace<V>> makeKey(K key) {
            //noinspection unchecked
            return (SlicedMapKey) delegate.makeKey(key);
        }

        @Override
        public WithStackTrace<V> computeValue(SlicedMap map, K key, WithStackTrace<V> value, boolean valueNotFound) {
            return new WithStackTrace<V>(delegate.computeValue(map, key, value == null ? null : value.value, valueNotFound));
        }

        @Override
        public ReadOnlySlice<K, WithStackTrace<V>> makeRawValueVersion() {
            return wrapSlice(delegate.makeRawValueVersion());
        }

        // Methods of WritableSlice

        private WritableSlice<K, V> getWritableDelegate() {
            return (WritableSlice<K, V>) delegate;
        }

        @Override
        public boolean isCollective() {
            return getWritableDelegate().isCollective();
        }

        @Override
        public RewritePolicy getRewritePolicy() {
            return getWritableDelegate().getRewritePolicy();
        }

        @Override
        public void afterPut(MutableSlicedMap map, K key, WithStackTrace<V> value) {
            getWritableDelegate().afterPut(map, key, value.value);
        }

        @Override
        public boolean check(K key, WithStackTrace<V> value) {
            return getWritableDelegate().check(key, value.value);
        }
    }
}
