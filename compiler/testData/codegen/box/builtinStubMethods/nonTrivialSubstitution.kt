import java.util.ArrayList

class MyCollection<T> : Collection<List<Iterator<T>>> {
    override fun iterator() = null!!
    override val size: Int get() = null!!
    override val isEmpty: Boolean get() = null!!
    override fun contains(o: List<Iterator<T>>): Boolean = null!!
    override fun containsAll(c: Collection<List<Iterator<T>>>): Boolean = null!!
}

fun box(): String {
    val c = MyCollection<String>() as java.util.Collection<List<Iterator<String>>>
    try {
        c.add(ArrayList())
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
