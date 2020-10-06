// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

class MySet<E> : Set<E> {
    val elements: ArrayList<E> = ArrayList<E>()

    override val size: Int get() = TODO()
    override fun contains(element: E): Boolean = TODO()
    override fun containsAll(elements: Collection<E>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()

    class MySetIterator<E>(elements: List<E>) : Iterator<E> {
        override fun hasNext(): Boolean = TODO()
        override fun next(): E = TODO()
    }

    override fun iterator() = MySetIterator(elements)
}