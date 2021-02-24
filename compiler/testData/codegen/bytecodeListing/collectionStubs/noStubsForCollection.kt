// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

class MyCollection<E> : Collection<E> {
    class MyIterator<E> : Iterator<E> {
        override fun hasNext(): Boolean = TODO()
        override fun next(): E = TODO()
    }

    override val size: Int get() = TODO()
    override fun contains(element: E): Boolean = TODO()
    override fun containsAll(elements: Collection<E>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator() = MyIterator<E>()
}