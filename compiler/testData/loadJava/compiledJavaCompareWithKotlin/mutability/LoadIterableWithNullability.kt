package test

public trait LoadIterableWithNullability<T> : java.lang.Object {
    public fun getIterable(): MutableIterable<T>
    public fun setIterable(p0: MutableIterable<T>)

    public fun getReadOnlyIterable(): Iterable<T>
    public fun setReadOnlyIterable(p0: Iterable<T>)
}
