package test;

import org.jetbrains.annotations.*;

public interface LoadIterableWithConflict<T> {
    public @ReadOnly @Mutable Iterable<T> getIterable();
    public void setIterable(@ReadOnly @Mutable Iterable<T> Iterable);
}
