package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.util.ReadOnlySlice;
import org.jetbrains.jet.util.WritableSlice;

import java.util.Map;

/**
 * @author abreslav
 */
public class BindingTraceAdapter implements BindingTrace {
    public interface RecordHandler<K, V> {
        void handleRecord(WritableSlice<K, V> slice, K key, V value);
    }

    private final BindingTrace originalTrace;
    private Map<WritableSlice, RecordHandler> handlers = Maps.newHashMap();

    public BindingTraceAdapter(BindingTrace originalTrace) {
        this.originalTrace = originalTrace;
    }

    @Override
    @NotNull
    public ErrorHandler getErrorHandler() {
        return originalTrace.getErrorHandler();
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
    
    public <K, V> BindingTraceAdapter addHandler(@NotNull WritableSlice<K, V> slice, @NotNull RecordHandler<K, V> handler) {
        handlers.put(slice, handler);
        return this;
    }
    
}