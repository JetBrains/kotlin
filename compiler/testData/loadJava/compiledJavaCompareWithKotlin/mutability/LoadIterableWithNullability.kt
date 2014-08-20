package test

import org.jetbrains.annotations.*

public trait LoadIterableWithNullability<T> {
    Mutable
    public fun getIterable(): MutableIterable<T>
    public fun setIterable([Mutable] p0: MutableIterable<T>)

    ReadOnly
    public fun getReadOnlyIterable(): Iterable<T>
    public fun setReadOnlyIterable([ReadOnly] p0: Iterable<T>)
}
