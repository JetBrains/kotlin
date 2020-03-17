open class BaseEmptyList<T> : List<T> {
    override val size: Int = 0
    override fun contains(element: T): Boolean = false
    override fun containsAll(elements: Collection<T>): Boolean = false
    override fun get(index: Int): T = error("Do not call")
    override fun indexOf(element: T): Int = -1
    override fun isEmpty(): Boolean = true
    override fun iterator(): Iterator<T> = emptyIterator()
    override fun lastIndexOf(element: T): Int = -1
    override fun listIterator(): ListIterator<T> = emptyIterator()
    override fun listIterator(index: Int): ListIterator<T> = emptyIterator()
    override fun subList(fromIndex: Int, toIndex: Int): List<T> = this

    private fun emptyIterator() = object : ListIterator<T> {
        override fun hasNext(): Boolean = false
        override fun next(): T = error("Do not call")
        override fun hasPrevious(): Boolean = false
        override fun nextIndex(): Int = 0
        override fun previous(): T = error("Do not call")
        override fun previousIndex(): Int = 0
    }
}

class DerivedEmptyList : BaseEmptyList<String>(), List<String>

// add() stub should be generated for BaseEmptyList, but not for DerivedEmptyList
// 1   public add\(ILjava/lang/Object;\)V
