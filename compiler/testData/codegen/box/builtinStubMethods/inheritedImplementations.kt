open class SetStringImpl {
    fun add(s: String): Boolean = false
    fun remove(o: Any?): Boolean = false
    fun clear(): Unit {}
}

class S : Set<String>, SetStringImpl() {
    override fun size(): Int = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: Any?): Boolean = false
    override fun iterator(): Iterator<String> = null!!
    override fun containsAll(c: Collection<Any?>) = false
}

fun box(): String {
    val s = S() as MutableSet<String>
    s.add("")
    s.remove("")
    s.clear()
    return "OK"
}
