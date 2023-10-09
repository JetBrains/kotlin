// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-62550

class MyList: List<String> {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: String): Boolean = false
    override fun iterator(): Iterator<String> = throw Error()
    override fun containsAll(c: Collection<String>): Boolean = false
    override fun get(index: Int): String = throw IndexOutOfBoundsException()
    override fun indexOf(o: String): Int = -1
    override fun lastIndexOf(o: String): Int = -1
    override fun listIterator(): ListIterator<String> = throw Error()
    override fun listIterator(index: Int): ListIterator<String> = throw Error()
    override fun subList(fromIndex: Int, toIndex: Int): List<String> = this
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = false
}

fun expectUoe(block: () -> Any) {
    try {
        block()
        throw AssertionError()
    } catch (e: UnsupportedOperationException) {
    }
}

fun box(): String {
    val list = MyList() as java.util.List<String>

    expectUoe { list.add("") }
    expectUoe { list.remove("") }
    expectUoe { list.addAll(list) }
    expectUoe { list.removeAll(list) }
    expectUoe { list.retainAll(list) }
    expectUoe { list.clear() }
    expectUoe { list.set(0, "") }
    expectUoe { list.add(0, "") }
    expectUoe { list.remove(0) }

    return "OK"
}
