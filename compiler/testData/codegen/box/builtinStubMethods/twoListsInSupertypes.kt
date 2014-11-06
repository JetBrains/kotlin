trait ListAny : List<Any>
trait ListString : List<String>

trait AddStringImpl {
    fun add(s: String) {}
}

class A : ListAny, ListString, AddStringImpl {
    override fun size(): Int = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: Any?): Boolean = false
    override fun iterator(): Iterator<String> = null!!
    override fun containsAll(c: Collection<Any?>): Boolean = false
    override fun get(index: Int): String = null!!
    override fun indexOf(o: Any?): Int = -1
    override fun lastIndexOf(o: Any?): Int = -1
    override fun listIterator(): ListIterator<String> = null!!
    override fun listIterator(index: Int): ListIterator<String> = null!!
    override fun subList(fromIndex: Int, toIndex: Int): List<String> = null!!
}

fun box(): String {
    val a = A() as MutableList<String>
    a.add("Fail")
    return "OK"
}
