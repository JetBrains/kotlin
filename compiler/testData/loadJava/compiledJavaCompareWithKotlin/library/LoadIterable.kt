package test

public trait LoadIterable<T> {
    public fun getIterable(): MutableIterable<T>?
    public fun setIterable(p0: Iterable<T>?)
}
