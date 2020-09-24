// WITH_RUNTIME

internal object EmptyList : List<Nothing>, RandomAccess {
    override fun equals(other: Any?): Boolean = TODO()
    override fun hashCode(): Int = 1
    override fun toString(): String = "[]"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun get(index: Int): Nothing = TODO()
    override fun indexOf(element: Nothing): Int = -1
    override fun lastIndexOf(element: Nothing): Int = -1

    override fun iterator(): Iterator<Nothing> = TODO()
    override fun listIterator(): ListIterator<Nothing> = TODO()
    override fun listIterator(index: Int): ListIterator<Nothing> { TODO() }

    override fun subList(fromIndex: Int, toIndex: Int): List<Nothing> { TODO() }
}