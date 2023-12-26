// JVM_ABI_K1_K2_DIFF: KT-63858

interface ImmutableCollection<out E> : Collection<E> {
    fun add(element: @UnsafeVariance E): ImmutableCollection<E>
    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E>
    fun remove(element: @UnsafeVariance E): ImmutableCollection<E>
}

class ImmutableCollectionmpl<E> : ImmutableCollection<E> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun contains(element: E): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun iterator(): Iterator<E> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun add(element: E): ImmutableCollection<E> = this
    override fun addAll(elements: Collection<E>): ImmutableCollection<E> = this
    override fun remove(element: E): ImmutableCollection<E> = this
}
