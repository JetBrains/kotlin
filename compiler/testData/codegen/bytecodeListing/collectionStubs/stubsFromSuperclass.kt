// WITH_STDLIB

open class A<T> : Collection<T> {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun contains(element: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<T> {
        TODO("Not yet implemented")
    }
}

open class B<E> : A<E>()
class C<F> : B<F>(), List<F> {
    override fun get(index: Int): F {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: F): Int {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: F): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<F> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<F> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<F> {
        TODO("Not yet implemented")
    }
}
