private object EmptyList : List<Nothing> {
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()
    override fun indexOf(element: Nothing): Int = -2
    override fun lastIndexOf(element: Nothing): Int = -2

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true

    override fun iterator(): Iterator<Nothing> = throw UnsupportedOperationException()
    override fun get(index: Int): Nothing = throw UnsupportedOperationException()
    override fun listIterator(): ListIterator<Nothing> = throw UnsupportedOperationException()
    override fun listIterator(index: Int): ListIterator<Nothing> = throw UnsupportedOperationException()
    override fun subList(fromIndex: Int, toIndex: Int): List<Nothing> = throw UnsupportedOperationException()
}

fun box(): String {
    val n = EmptyList as List<Any?>

    if (n.contains(null)) return "fail 1"
    if (n.indexOf(null) != -1) return "fail 2"
    if (n.lastIndexOf(null) != -1) return "fail 3"

    return "OK"
}
