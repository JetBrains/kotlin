// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

// JVM_ABI_K1_K2_DIFF: KT-63864
class MyList<E> : MutableList<E> {
    val elements = ArrayList<E>()

    class MyListIterator<E>(
        val list: ArrayList<E>,
        start: Int,
        private val end: Int
    ) : MutableListIterator<E> {
        var index = start
        override fun hasNext() = index < end
        override fun next() = list[index++]
        override fun hasPrevious() = index > 0
        override fun nextIndex() = index + 1
        override fun previous() = list[--index]
        override fun previousIndex() = index - 1

        override fun add(element: E): Unit = TODO()
        override fun remove(): Unit = TODO()
        override fun set(element: E): Unit = TODO()
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

    // MutableList operations
    override fun add(element: E): Boolean = TODO()
    override fun add(index: Int, element: E): Unit = TODO()
    override fun addAll(index: Int, elements: Collection<E>): Boolean = TODO()
    override fun addAll(elements: Collection<E>): Boolean = TODO()
    override fun clear(): Unit = TODO()
    override fun remove(element: E): Boolean = TODO()
    override fun removeAll(elements: Collection<E>): Boolean = TODO()
    override fun removeAt(index: Int): E = TODO()
    override fun retainAll(elements: Collection<E>): Boolean = TODO()
    override fun set(index: Int, element: E): E = TODO()
}