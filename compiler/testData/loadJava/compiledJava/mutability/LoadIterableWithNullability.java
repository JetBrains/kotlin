// JVM_ANNOTATIONS
package test;

import kotlin.annotations.jvm.*;
import org.jetbrains.annotations.NotNull;

public interface LoadIterableWithNullability<T> {
    public @NotNull @Mutable Iterable<T> getIterable();
    public void setIterable(@Mutable @NotNull Iterable<T> Iterable);

    public @NotNull @ReadOnly Iterable<T> getReadOnlyIterable();
    public void setReadOnlyIterable(@ReadOnly @NotNull Iterable<T> Iterable);
}
