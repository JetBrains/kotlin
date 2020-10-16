// IGNORE_ANNOTATIONS

inline class IT(val x: Long)

inline class InlineMutableList(private val mlist: MutableList<IT>) : MutableList<IT> {
    override val size: Int get() = mlist.size
    override fun contains(element: IT): Boolean = mlist.contains(element)
    override fun containsAll(elements: Collection<IT>): Boolean = mlist.containsAll(elements)
    override fun get(index: Int): IT = mlist[index]
    override fun indexOf(element: IT): Int = mlist.indexOf(element)
    override fun isEmpty(): Boolean = mlist.isEmpty()
    override fun iterator(): MutableIterator<IT> = mlist.iterator()
    override fun lastIndexOf(element: IT): Int = mlist.lastIndexOf(element)
    override fun add(element: IT): Boolean = mlist.add(element)
    override fun add(index: Int, element: IT) { mlist.add(index, element) }
    override fun addAll(index: Int, elements: Collection<IT>): Boolean = mlist.addAll(index, elements)
    override fun addAll(elements: Collection<IT>): Boolean = mlist.addAll(elements)
    override fun clear() { mlist.clear() }
    override fun listIterator(): MutableListIterator<IT> = mlist.listIterator()
    override fun listIterator(index: Int): MutableListIterator<IT> = mlist.listIterator(index)
    override fun remove(element: IT): Boolean = mlist.remove(element)
    override fun removeAll(elements: Collection<IT>): Boolean = mlist.removeAll(elements)
    override fun removeAt(index: Int): IT = mlist.removeAt(index)
    override fun retainAll(elements: Collection<IT>): Boolean = mlist.retainAll(elements)
    override fun set(index: Int, element: IT): IT = mlist.set(index, element)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<IT> = mlist.subList(fromIndex, toIndex)
}
