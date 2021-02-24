// IGNORE_ANNOTATIONS

class MutableListOfLong(private val ml: MutableList<Long>) : MutableList<Long> {
    override val size: Int get() = ml.size
    override fun contains(element: Long): Boolean = ml.contains(element)
    override fun containsAll(elements: Collection<Long>): Boolean = ml.containsAll(elements)
    override fun get(index: Int): Long = ml.get(index)
    override fun indexOf(element: Long): Int = ml.indexOf(element)
    override fun isEmpty(): Boolean = ml.isEmpty()
    override fun iterator(): MutableIterator<Long> = ml.iterator()
    override fun lastIndexOf(element: Long): Int = ml.lastIndexOf(element)
    override fun add(element: Long): Boolean = ml.add(element)
    override fun add(index: Int, element: Long) = ml.add(index, element)
    override fun addAll(index: Int, elements: Collection<Long>): Boolean = ml.addAll(index, elements)
    override fun addAll(elements: Collection<Long>): Boolean = ml.addAll(elements)
    override fun clear() = ml.clear()
    override fun listIterator(): MutableListIterator<Long> = ml.listIterator()
    override fun listIterator(index: Int): MutableListIterator<Long> = ml.listIterator(index)
    override fun remove(element: Long): Boolean = ml.remove(element)
    override fun removeAll(elements: Collection<Long>): Boolean = ml.removeAll(elements)
    override fun removeAt(index: Int): Long = ml.removeAt(index)
    override fun retainAll(elements: Collection<Long>): Boolean = ml.retainAll(elements)
    override fun set(index: Int, element: Long): Long = ml.set(index, element)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Long> = ml.subList(fromIndex, toIndex)
}