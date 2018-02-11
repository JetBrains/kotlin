// JVM_ANNOTATIONS
package test

import kotlin.annotations.jvm.*

public interface LoadIterableWithConflict<T> {
    @ReadOnly @Mutable
    public fun getIterable(): MutableIterable<T>?
    public fun setIterable(@ReadOnly @Mutable p0: MutableIterable<T>?)
}
