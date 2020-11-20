// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.
class MySet<E> : MutableSet<E> {
    val elements: ArrayList<E> = ArrayList<E>()

    override val size: Int
        get() = TODO()

    override fun add(element: E): Boolean = TODO()
    override fun addAll(elements: Collection<E>): Boolean = TODO()
    override fun clear(): Unit = TODO()
    override fun remove(element: E): Boolean = TODO()
    override fun removeAll(elements: Collection<E>): Boolean = TODO()
    override fun retainAll(elements: Collection<E>): Boolean = TODO()
    override fun contains(element: E): Boolean = TODO()
    override fun containsAll(elements: Collection<E>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()

    class MySetIterator<E>(elements: List<E>) : MutableIterator<E> {
        override fun hasNext(): Boolean = TODO()
        override fun next(): E = TODO()
        override fun remove(): Unit = TODO()
    }

    override fun iterator() = MySetIterator(elements)
}