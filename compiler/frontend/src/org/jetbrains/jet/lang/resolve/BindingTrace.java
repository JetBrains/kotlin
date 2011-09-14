package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder;
import org.jetbrains.jet.lang.diagnostics.ErrorHandler;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

/**
 * @author abreslav
 */
public interface BindingTrace extends DiagnosticHolder {

    @NotNull
    ErrorHandler getErrorHandler();

    BindingContext getBindingContext();
    
    <K, V> void record(WritableSlice<K, V> slice, K key, V value);

    // Writes TRUE for a boolean value
    <K> void record(WritableSlice<K, Boolean> slice, K key);

    @Nullable
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);
}
