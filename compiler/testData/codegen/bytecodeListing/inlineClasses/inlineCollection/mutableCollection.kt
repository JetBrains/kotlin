// IGNORE_ANNOTATIONS

inline class InlineMutableCollection<T>(private val mc: MutableCollection<T>) : MutableCollection<T> {
    override val size: Int get() = mc.size
    override fun contains(element: T): Boolean = mc.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = mc.containsAll(elements)
    override fun isEmpty(): Boolean = mc.isEmpty()
    override fun add(element: T): Boolean = mc.add(element)
    override fun addAll(elements: Collection<T>): Boolean = mc.addAll(elements)
    override fun clear() { mc.clear() }
    override fun iterator(): MutableIterator<T> = mc.iterator()
    override fun remove(element: T): Boolean = mc.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = mc.removeAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = mc.retainAll(elements)
}

