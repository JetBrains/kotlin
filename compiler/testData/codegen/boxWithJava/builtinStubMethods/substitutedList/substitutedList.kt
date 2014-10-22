abstract class C : Test.A, List<String> {
    override fun size(): Int = null!!
    override fun isEmpty(): Boolean = null!!
    override fun contains(o: Any?): Boolean = null!!
    override fun iterator(): Iterator<String> = null!!
    override fun containsAll(c: Collection<Any?>): Boolean = null!!
    override fun get(index: Int): String = null!!
    override fun indexOf(o: Any?): Int = null!!
    override fun lastIndexOf(o: Any?): Int = null!!
    override fun listIterator(): ListIterator<String> = null!!
    override fun listIterator(index: Int): ListIterator<String> = null!!
    override fun subList(fromIndex: Int, toIndex: Int): List<String> = null!!
}

fun box(): String {
    try {
        Test().test()
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
