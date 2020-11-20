class IntMutableCollection(private val mc: MutableCollection<Int>) : MutableCollection<Int> {
    override val size: Int get() = mc.size
    override fun contains(element: Int): Boolean = mc.contains(element)
    override fun containsAll(elements: Collection<Int>): Boolean = mc.containsAll(elements)
    override fun isEmpty(): Boolean = mc.isEmpty()
    override fun add(element: Int): Boolean = mc.add(element)
    override fun addAll(elements: Collection<Int>): Boolean = mc.addAll(elements)
    override fun clear() { mc.clear() }
    override fun iterator(): MutableIterator<Int> = mc.iterator()
    override fun remove(element: Int): Boolean = mc.remove(element)
    override fun removeAll(elements: Collection<Int>): Boolean = mc.removeAll(elements)
    override fun retainAll(elements: Collection<Int>): Boolean = mc.retainAll(elements)
}

class LongMutableCollection(private val mc: MutableCollection<Long>) : MutableCollection<Long> {
    override val size: Int get() = mc.size
    override fun contains(element: Long): Boolean = mc.contains(element)
    override fun containsAll(elements: Collection<Long>): Boolean = mc.containsAll(elements)
    override fun isEmpty(): Boolean = mc.isEmpty()
    override fun add(element: Long): Boolean = mc.add(element)
    override fun addAll(elements: Collection<Long>): Boolean = mc.addAll(elements)
    override fun clear() { mc.clear() }
    override fun iterator(): MutableIterator<Long> = mc.iterator()
    override fun remove(element: Long): Boolean = mc.remove(element)
    override fun removeAll(elements: Collection<Long>): Boolean = mc.removeAll(elements)
    override fun retainAll(elements: Collection<Long>): Boolean = mc.retainAll(elements)
}