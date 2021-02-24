// WITH_RUNTIME
// IGNORE_ANNOTATIONS

inline class UIntArray(@PublishedApi internal val storage: IntArray) : Collection<UInt> {
    override val size: Int get() = TODO()
    override operator fun iterator() = TODO()
    override fun contains(element: UInt): Boolean = TODO()
    override fun containsAll(elements: Collection<UInt>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
}