// FIR_IDENTICAL
// ISSUE: KT-60770

// Should be no ABSTRACT_MEMBER_NOT_IMPLEMENTED
class B<F> : List<F> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun contains(element: F): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(elements: Collection<F>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): F {
        throw UnsupportedOperationException()
    }

    override fun indexOf(element: F): Int {
        throw UnsupportedOperationException()
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterator(): Iterator<F> {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(element: F): Int {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): ListIterator<F> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): ListIterator<F> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<F> {
        throw UnsupportedOperationException()
    }
}
