// FULL_JDK
package test

class EmptyStringList : List<String> {
    override val size: Int get() = 0
    override fun get(index: Int): String = error("empty")
    override fun indexOf(element: String): Int = -1
    override fun contains(element: String): Boolean = false
    override fun containsAll(elements: Collection<String>): Boolean = false
    override fun isEmpty(): Boolean = true
    override fun iterator(): Iterator<String> = error("empty")
    override fun listIterator(): ListIterator<String> = error("empty")
    override fun listIterator(index: Int): ListIterator<String> = error("empty")
    override fun subList(fromIndex: Int, toIndex: Int): List<String> = this
    override fun lastIndexOf(element: String): Int = -1
}
