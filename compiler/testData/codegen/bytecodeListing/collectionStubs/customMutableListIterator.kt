// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.
class MyList<E> : MutableList<E> {
    private val elements = ArrayList<E>()

    class MyListIterator<E>(
        private val list: ArrayList<E>,
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

        override fun add(element: E) = TODO("not implemented")
        override fun remove() = TODO("not implemented")
        override fun set(element: E) = TODO("not implemented")
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
    override fun add(element: E) = TODO("not implemented")
    override fun add(index: Int, element: E) = TODO("not implemented")
    override fun addAll(elements: Collection<E>) = TODO("not implemented")
    override fun addAll(index: Int, elements: Collection<E>) = TODO("not implemented")
    override fun clear() = TODO("not implemented")
    override fun remove(element: E) = TODO("not implemented")
    override fun removeAt(index: Int) = TODO("not implemented")
    override fun removeAll(elements: Collection<E>) = TODO("not implemented")
    override fun retainAll(elements: Collection<E>) = TODO("not implemented")
    override fun set(index: Int, element: E) = TODO("not implemented")
}