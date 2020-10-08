// IGNORE_ANNOTATIONS

inline class InlineMutableList<T>(private val mlist: MutableList<T>) : MutableList<T> {
    override val size: Int get() = mlist.size
    override fun contains(element: T): Boolean = mlist.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = mlist.containsAll(elements)
    override fun get(index: Int): T = mlist[index]
    override fun indexOf(element: T): Int = mlist.indexOf(element)
    override fun isEmpty(): Boolean = mlist.isEmpty()
    override fun iterator(): MutableIterator<T> = mlist.iterator()
    override fun lastIndexOf(element: T): Int = mlist.lastIndexOf(element)
    override fun add(element: T): Boolean = mlist.add(element)
    override fun add(index: Int, element: T) { mlist.add(index, element) }
    override fun addAll(index: Int, elements: Collection<T>): Boolean = mlist.addAll(index, elements)
    override fun addAll(elements: Collection<T>): Boolean = mlist.addAll(elements)
    override fun clear() { mlist.clear() }
    override fun listIterator(): MutableListIterator<T> = mlist.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = mlist.listIterator(index)
    override fun remove(element: T): Boolean = mlist.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = mlist.removeAll(elements)
    override fun removeAt(index: Int): T = mlist.removeAt(index)
    override fun retainAll(elements: Collection<T>): Boolean = mlist.retainAll(elements)
    override fun set(index: Int, element: T): T = mlist.set(index, element)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = mlist.subList(fromIndex, toIndex)
}

