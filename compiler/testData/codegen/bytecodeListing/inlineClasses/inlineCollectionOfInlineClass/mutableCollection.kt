// IGNORE_ANNOTATIONS
// IGNORE_BACKEND: JVM_IR
// ^ TODO: special bridges <-> inline classes interaction

inline class IT(val x: Int)

inline class InlineMutableCollection(private val mc: MutableCollection<IT>) : MutableCollection<IT> {
    override val size: Int get() = mc.size
    override fun contains(element: IT): Boolean = mc.contains(element)
    override fun containsAll(elements: Collection<IT>): Boolean = mc.containsAll(elements)
    override fun isEmpty(): Boolean = mc.isEmpty()
    override fun add(element: IT): Boolean = mc.add(element)
    override fun addAll(elements: Collection<IT>): Boolean = mc.addAll(elements)
    override fun clear() { mc.clear() }
    override fun iterator(): MutableIterator<IT> = mc.iterator()
    override fun remove(element: IT): Boolean = mc.remove(element)
    override fun removeAll(elements: Collection<IT>): Boolean = mc.removeAll(elements)
    override fun retainAll(elements: Collection<IT>): Boolean = mc.retainAll(elements)
}

