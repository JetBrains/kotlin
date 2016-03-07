// WITH_RUNTIME

interface Stream<T> {
    fun iterator(): Iterator<T>
}

class ZippingStream<T1, T2>(val stream1: Stream<T1>, val stream2: Stream<T2>) : Stream<Pair<T1,T2>> {
    override fun iterator(): Iterator<Pair<T1,T2>> = object : AbstractIterator<Pair<T1,T2>>() {
        val iterator1 = stream1.iterator()
        val iterator2 = stream2.iterator()
        override fun computeNext() {
            if (iterator1.hasNext() && iterator2.hasNext()) {
                setNext(iterator1.next() to iterator2.next())
            } else {
                done()
            }
        }
    }
}


object EmptyStream : Stream<Nothing> {
    override fun iterator() = listOf<Nothing>().iterator()
}

fun box(): String {
    ZippingStream(EmptyStream, EmptyStream).iterator().hasNext()
    return "OK"
}
