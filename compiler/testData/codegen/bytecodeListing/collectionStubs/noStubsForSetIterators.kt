// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

class MySet<E> : Set<E> {
    val elements: ArrayList<E> = ArrayList<E>()

    override val size: Int
        get() = TODO("not implemented")

    override fun contains(element: E) = TODO("not implemented")
    override fun containsAll(elements: Collection<E>) = TODO("not implemented")
    override fun isEmpty() = TODO("not implemented")

    class MySetIterator<E>(elements: List<E>) : Iterator<E> {
        override fun hasNext() = TODO("not implemented")
        override fun next() = TODO("not implemented")
    }

    override fun iterator() = MySetIterator(elements)
}