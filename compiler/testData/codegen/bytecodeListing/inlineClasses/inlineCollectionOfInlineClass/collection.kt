// IGNORE_ANNOTATIONS

inline class IT(val x: Int)

inline class InlineCollection(private val c: Collection<IT>) : Collection<IT> {
    override val size: Int get() = c.size
    override fun contains(element: IT): Boolean = c.contains(element)
    override fun containsAll(elements: Collection<IT>): Boolean = c.containsAll(elements)
    override fun isEmpty(): Boolean = c.isEmpty()
    override fun iterator(): Iterator<IT> = c.iterator()
}
