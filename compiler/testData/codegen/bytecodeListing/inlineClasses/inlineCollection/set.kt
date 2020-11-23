// IGNORE_ANNOTATIONS

inline class InlineSet<T>(private val s: Set<T>) : Set<T> {
    override val size: Int get() = s.size
    override fun contains(element: T): Boolean = s.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = s.containsAll(elements)
    override fun isEmpty(): Boolean = s.isEmpty()
    override fun iterator(): Iterator<T> = s.iterator()
}

