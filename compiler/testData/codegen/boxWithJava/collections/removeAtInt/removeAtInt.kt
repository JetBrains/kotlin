open class A : MutableList<Int> {
    override val size: Int
        get() = throw UnsupportedOperationException()
    override val isEmpty: Boolean
        get() = throw UnsupportedOperationException()

    override fun contains(o: Int): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<Int>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun indexOf(o: Any?): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(o: Any?): Int {
        throw UnsupportedOperationException()
    }

    override fun add(e: Int): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: Int) = true

    override fun removeAt(index: Int): Int = 123

    override fun addAll(c: Collection<Int>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, c: Collection<Int>): Boolean {
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

    override fun set(index: Int, element: Int): Int {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: Int) {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): MutableListIterator<Int> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): MutableListIterator<Int> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Int> {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<Int> {
        throw UnsupportedOperationException()
    }
}

fun box() = J.foo()