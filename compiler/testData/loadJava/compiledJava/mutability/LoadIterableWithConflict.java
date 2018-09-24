// JVM_ANNOTATIONS
package test;

import kotlin.annotations.jvm.*;

public interface LoadIterableWithConflict<T> {
    public @ReadOnly @Mutable Iterable<T> getIterable();
    public void setIterable(@ReadOnly @Mutable Iterable<T> Iterable);
}
