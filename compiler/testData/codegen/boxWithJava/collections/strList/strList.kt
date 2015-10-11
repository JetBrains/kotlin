open class KList : MutableList<String> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override val isEmpty: Boolean get() {
        throw UnsupportedOperationException()
    }

    override fun contains(o: String) = true
    override fun containsAll(c: Collection<String>) = true

    override fun get(index: Int): String {
        throw UnsupportedOperationException()
    }

    override fun indexOf(o: Any?): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(o: Any?): Int {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun add(e: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, c: Collection<String>): Boolean {
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

    override fun set(index: Int, element: String): String {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: String) {
        throw UnsupportedOperationException()
    }

    override fun removeAt(index: Int): String {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<String> {
        throw UnsupportedOperationException()
    }

}
fun box() = J.foo()
