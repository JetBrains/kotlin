// IGNORE_ANNOTATIONS

inline class IT(val x: Int)

inline class InlineList(private val list: List<IT>) : List<IT> {
    override val size: Int get() = list.size
    override fun contains(element: IT): Boolean = list.contains(element)
    override fun containsAll(elements: Collection<IT>): Boolean = list.containsAll(elements)
    override fun get(index: Int): IT = list[index]
    override fun indexOf(element: IT): Int = list.indexOf(element)
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun iterator(): Iterator<IT> = list.iterator()
    override fun lastIndexOf(element: IT): Int = list.lastIndexOf(element)
    override fun listIterator(): ListIterator<IT> = list.listIterator()
    override fun listIterator(index: Int): ListIterator<IT> = list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<IT> = list.subList(fromIndex, toIndex)
}
