import java.util.ArrayList

class MyCollection<T> : Collection<List<Iterator<T>>> {
    override fun iterator() = null!!
    override fun size(): Int = null!!
    override fun isEmpty(): Boolean = null!!
    override fun contains(o: Any?): Boolean = null!!
    override fun containsAll(c: Collection<Any?>): Boolean = null!!
}

fun box(): String {
    val c = MyCollection<String>() as MutableCollection<List<Iterator<String>>>
    try {
        c.add(ArrayList())
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
