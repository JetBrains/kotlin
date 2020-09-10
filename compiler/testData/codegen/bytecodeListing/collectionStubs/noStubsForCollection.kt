// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

class MyCollection<E> : Collection<E> {
    class MyIterator<E> : Iterator<E> {
        override fun hasNext(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun next(): E {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override val size: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun contains(element: E): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator() = MyIterator<E>()
}