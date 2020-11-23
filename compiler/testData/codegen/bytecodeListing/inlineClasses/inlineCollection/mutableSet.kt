// IGNORE_ANNOTATIONS

inline class InlineMutableSet<T>(private val ms: MutableSet<T>) : MutableSet<T> {
    override val size: Int get() = ms.size
    override fun contains(element: T): Boolean = ms.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = ms.containsAll(elements)
    override fun isEmpty(): Boolean = ms.isEmpty()
    override fun add(element: T): Boolean = ms.add(element)
    override fun addAll(elements: Collection<T>): Boolean = ms.addAll(elements)
    override fun clear() { ms.clear() }
    override fun iterator(): MutableIterator<T> = ms.iterator()
    override fun remove(element: T): Boolean = ms.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = ms.removeAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = ms.retainAll(elements)
}

