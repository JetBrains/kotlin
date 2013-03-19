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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.util.slicedmap.*;

import java.util.Collection;
import java.util.List;

public class BindingTraceContext implements BindingTrace {
    private final List<Diagnostic> diagnostics = Lists.newArrayList();

    // This flag is used for debugging of "Rewrite at slice..." exceptions
    // NOTE: sometimes TrackingSlicedMap throws a ClassCastException (after you have fixed the rewrite).
    // I gave up debugging it, because it still serves its purpose. Any suggestions on how to fix it are welcome. (abreslav)
    private final static boolean TRACK_REWRITES = false;
    @SuppressWarnings("ConstantConditions")
    private final MutableSlicedMap map = TRACK_REWRITES ? new TrackingSlicedMap(SlicedMapImpl.create()) : SlicedMapImpl.create();

    private final BindingContext bindingContext = new BindingContext() {

        @Override
        public Collection<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return BindingTraceContext.this.get(slice, key);
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return BindingTraceContext.this.getKeys(slice);
        }

        @NotNull
        @TestOnly
        @Override
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            return map.getSliceContents(slice);
        }
    };

    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public void clearDiagnostics() {
        diagnostics.clear();
    }

    @Override
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @Override
    public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        map.put(slice, key, value);
    }

    @Override
    public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        record(slice, key, true);
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        return map.get(slice, key);
    }

    @NotNull
    @Override
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        return map.getKeys(slice);
    }
}
