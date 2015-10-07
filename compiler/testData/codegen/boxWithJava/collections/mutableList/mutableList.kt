open class KList<E> : MutableList<E> {
    override fun add(e: E): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: Any?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(c: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, c: Collection<E>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(c: Collection<Any?>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(c: Collection<Any?>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun set(index: Int, element: E): E {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: E) {
        throw UnsupportedOperationException()
    }

    override fun remove(index: Int): E {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): MutableListIterator<E> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<E> {
        throw UnsupportedOperationException()
    }

    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun contains(o: E) = true
    override fun containsAll(c: Collection<E>) = true

    override fun get(index: Int): E {
        throw UnsupportedOperationException()
    }

    override fun indexOf(o: Any?): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(o: Any?): Int {
        throw UnsupportedOperationException()
    }
}

fun box() = J.foo()
