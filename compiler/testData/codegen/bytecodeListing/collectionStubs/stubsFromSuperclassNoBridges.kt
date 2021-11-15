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

open class B<E : CharSequence> : A<E>()
class C : B<CharSequence>(), List<CharSequence> {
    override fun get(index: Int): CharSequence {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: CharSequence): Int {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: CharSequence): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<CharSequence> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<CharSequence> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<CharSequence> {
        TODO("Not yet implemented")
    }
}
