package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.CollectingErrorHandler;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetDiagnostic;
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
    private final List<JetDiagnostic> diagnostics = Lists.newArrayList();
    private final ErrorHandler errorHandler = new CollectingErrorHandler(diagnostics);

    private final BindingContext bindingContext = new BindingContext() {
        @Override
        public Collection<JetDiagnostic> getDiagnostics() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return DelegatingBindingTrace.this.get(slice, key);
        }
    };

    public DelegatingBindingTrace(BindingContext parentContext) {
        this.parentContext = parentContext;
    }

    @Override
    @NotNull
    public ErrorHandler getErrorHandler() {
        return errorHandler;
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

    public void addAllMyDataTo(BindingTrace trace) {
        for (Map.Entry<SlicedMapKey<?, ?>, ?> entry : map) {
            SlicedMapKey slicedMapKey = entry.getKey();
            Object value = entry.getValue();

            //noinspection unchecked
            trace.record(slicedMapKey.getSlice(), slicedMapKey.getKey(), value);
        }
        
        ErrorHandler.applyHandler(trace.getErrorHandler(), diagnostics);
    }
}