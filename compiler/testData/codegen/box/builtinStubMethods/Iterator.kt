// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

class MyIterator<T>(val v: T): Iterator<T> {
    override fun next(): T = v
    override fun hasNext(): Boolean = true
}

fun box(): String {
    try {
        (MyIterator<String>("") as java.util.Iterator<String>).remove()
        throw AssertionError()
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
