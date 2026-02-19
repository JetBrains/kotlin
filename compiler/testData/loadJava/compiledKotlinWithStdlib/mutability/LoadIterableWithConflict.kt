// WITH_KOTLIN_JVM_ANNOTATIONS
// TARGET_BACKEND: JVM
package test

import kotlin.annotations.jvm.*

public interface LoadIterableWithConflict<T> {
    @ReadOnly @Mutable
    public fun getIterable(): MutableIterable<T>?
    public fun setIterable(@ReadOnly @Mutable p0: MutableIterable<T>?)
}
