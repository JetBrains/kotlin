class MyIterator<E : Number> : Iterator<E> {
    override fun next() = null!!
    override fun hasNext() = null!!
}

fun box(): String {
    try {
        (MyIterator<Int>() as MutableIterator<Number>).remove()
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
