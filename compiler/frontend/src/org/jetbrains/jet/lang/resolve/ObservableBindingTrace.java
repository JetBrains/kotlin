/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;
import java.util.Map;

public class ObservableBindingTrace implements BindingTrace {
    public interface RecordHandler<K, V> {

        void handleRecord(WritableSlice<K, V> slice, K key, V value);
    }

    private final BindingTrace originalTrace;

    private Map<WritableSlice, RecordHandler> handlers = Maps.newHashMap();

    public ObservableBindingTrace(BindingTrace originalTrace) {
        this.originalTrace = originalTrace;
    }
    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        originalTrace.report(diagnostic);
    }

    @Override
    public BindingContext getBindingContext() {
        return originalTrace.getBindingContext();
    }

    @Override
    public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        originalTrace.record(slice, key, value);
        RecordHandler recordHandler = handlers.get(slice);
        if (recordHandler != null) {
            recordHandler.handleRecord(slice, key, value);
        }
    }

    @Override
    public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        record(slice, key, true);
    }

    @Override
    public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
        return originalTrace.get(slice, key);
    }

    @Override
    @NotNull
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        return originalTrace.getKeys(slice);
    }

    public <K, V> ObservableBindingTrace addHandler(@NotNull WritableSlice<K, V> slice, @NotNull RecordHandler<K, V> handler) {
        handlers.put(slice, handler);
        return this;
    }
    
}
