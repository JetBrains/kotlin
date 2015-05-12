package test

public interface LoadIterable<T> {
    public fun getIterable(): MutableIterable<T>?
    public fun setIterable(p0: Iterable<T>?)
}
