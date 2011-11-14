package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.util.slicedmap.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class DelegatingBindingTrace implements BindingTrace {
    private final BindingContext parentContext;
    private final MutableSlicedMap map = SlicedMapImpl.create();
    private final List<Diagnostic> diagnostics = Lists.newArrayList();

    private final BindingContext bindingContext = new BindingContext() {
        @Override
        public Collection<Diagnostic> getDiagnostics() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return DelegatingBindingTrace.this.get(slice, key);
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return DelegatingBindingTrace.this.getKeys(slice);

        }
    };

    public DelegatingBindingTrace(BindingContext parentContext) {
        this.parentContext = parentContext;
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
        if (map.containsKey(slice, key)) {
            return map.get(slice, key);
        }
        return parentContext.get(slice, key);
    }

    @NotNull
    @Override
    public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
        Collection<K> keys = map.getKeys(slice);
        Collection<K> fromParent = parentContext.getKeys(slice);
        if (keys.isEmpty()) return fromParent;
        if (fromParent.isEmpty()) return keys;

        List<K> result = Lists.newArrayList(keys);
        result.addAll(fromParent);
        return result;
    }

    public void addAllMyDataTo(BindingTrace trace) {
        for (Map.Entry<SlicedMapKey<?, ?>, ?> entry : map) {
            SlicedMapKey slicedMapKey = entry.getKey();
            Object value = entry.getValue();

            //noinspection unchecked
            trace.record(slicedMapKey.getSlice(), slicedMapKey.getKey(), value);
        }
        
        for (Diagnostic diagnostic : diagnostics) {
            trace.report(diagnostic);
        }
    }

    public void clear() {
        map.clear();
        diagnostics.clear();
    }

    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }
}