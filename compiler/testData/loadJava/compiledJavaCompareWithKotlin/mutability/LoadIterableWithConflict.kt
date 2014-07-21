package test

import org.jetbrains.annotations.*

public trait LoadIterableWithConflict<T> {
    [ReadOnly] [Mutable]
    public fun getIterable(): MutableIterable<T>?
    public fun setIterable([ReadOnly] [Mutable] p0: MutableIterable<T>?)
}
