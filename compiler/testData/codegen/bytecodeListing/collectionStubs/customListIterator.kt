// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

// JVM_ABI_K1_K2_DIFF: KT-63864

class MyList<E> : List<E> {
    val elements = ArrayList<E>()

    class MyListIterator<E>(
        val list: ArrayList<E>,
        start: Int,
        private val end: Int
    ) : ListIterator<E> {
        var index = start
        override fun hasNext() = index < end
        override fun next() = list[index++]
        override fun hasPrevious() = index > 0
        override fun nextIndex() = index + 1
        override fun previous() = list[--index]
        override fun previousIndex() = index - 1
    }

    // List<E> implementation:
    override fun listIterator(index: Int) = MyListIterator(elements, index, size)
    override fun listIterator() = listIterator(0)
    override fun iterator() = listIterator()

    override val size get() = elements.size
    override fun contains(element: E) = elements.contains(element)
    override fun containsAll(elements: Collection<E>) = this.elements.containsAll(elements)
    override operator fun get(index: Int) = elements[index]
    override fun indexOf(element: E): Int = elements.indexOf(element)
    override fun lastIndexOf(element: E): Int = elements.lastIndexOf(element)
    override fun isEmpty() = elements.isEmpty()
    override fun subList(fromIndex: Int, toIndex: Int) = elements.subList(fromIndex, toIndex)
}