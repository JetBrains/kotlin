package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

public interface TraceEntryFilter {
    boolean accept(@NotNull WritableSlice<?, ?> slice, Object key);
}
