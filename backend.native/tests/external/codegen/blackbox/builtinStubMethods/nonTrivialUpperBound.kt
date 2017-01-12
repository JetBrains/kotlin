// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class MyIterator<E : Number> : Iterator<E> {
    override fun next() = null!!
    override fun hasNext() = null!!
}

fun box(): String {
    try {
        (MyIterator<Int>() as java.util.Iterator<Number>).remove()
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
