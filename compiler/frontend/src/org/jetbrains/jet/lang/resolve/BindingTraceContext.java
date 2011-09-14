package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.CollectingErrorHandler;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.ErrorHandler;
import org.jetbrains.jet.lang.diagnostics.JetDiagnostic;
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
    private final List<Diagnostic> diagnostics = Lists.newArrayList();
    private final List<JetDiagnostic> old_diagnostics = Lists.newArrayList();
    private final ErrorHandler errorHandler = new CollectingErrorHandler(old_diagnostics);

    private final MutableSlicedMap map = SlicedMapImpl.create();

    private final BindingContext bindingContext = new BindingContext() {
        @Override
        public Collection<JetDiagnostic> getOld_Diagnostics() {
            return old_diagnostics;
        }

        @Override
        public Collection<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return BindingTraceContext.this.get(slice, key);
        }
    };

    @Override
    public void report(@NotNull Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

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