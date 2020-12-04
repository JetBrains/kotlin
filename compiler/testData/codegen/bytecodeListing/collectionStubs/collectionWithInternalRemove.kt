class Test<T> : Collection<T> {
    override val size: Int get() = TODO()
    override fun contains(element: T): Boolean = TODO()
    override fun containsAll(elements: Collection<T>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<T> = TODO()

    internal fun remove(x: T): Boolean = false
}