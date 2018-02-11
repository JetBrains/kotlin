// JVM_ANNOTATIONS
package test;

import kotlin.annotations.jvm.*;

public interface LoadIterable<T> {
    public @Mutable Iterable<T> getIterable();
    public void setIterable(@Mutable Iterable<T> Iterable);

    public @ReadOnly Iterable<T> getReadOnlyIterable();
    public void setReadOnlyIterable(@ReadOnly Iterable<T> Iterable);
}
