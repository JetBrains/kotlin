class MyIterator<T>(val v: T): Iterator<T> {
    override fun next(): T = v
    override fun hasNext(): Boolean = true

    public fun remove() {}
}

fun box(): String {
    (MyIterator<String>("") as MutableIterator<String>).remove()
    return "OK"
}
