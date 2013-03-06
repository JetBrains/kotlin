package test

public trait LoadIterator<T> : java.lang.Object {
    public fun getIterator(): MutableIterator<T>?
    public fun setIterator(p0: Iterator<T>?)
}
