package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.CollectingErrorHandler;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetDiagnostic;
import org.jetbrains.jet.util.slicedmap.MutableSlicedMap;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.SlicedMapImpl;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class BindingTraceContext implements BindingTrace {
    private final List<JetDiagnostic> diagnostics = Lists.newArrayList();
    private final ErrorHandler errorHandler = new CollectingErrorHandler(diagnostics);

    private final MutableSlicedMap map = SlicedMapImpl.create();

    private final BindingContext bindingContext = new BindingContext() {
        @Override
        public Collection<JetDiagnostic> getDiagnostics() {
            return diagnostics;
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return BindingTraceContext.this.get(slice, key);
        }
    };

    @NotNull
    @Override
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
        return map.get(slice, key);
    }
}