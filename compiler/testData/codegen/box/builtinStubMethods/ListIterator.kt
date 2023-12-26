// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-62550

class MyListIterator<T> : ListIterator<T> {
    override fun next(): T = null!!
    override fun hasNext(): Boolean = null!!
    override fun hasPrevious(): Boolean = null!!
    override fun previous(): T = null!!
    override fun nextIndex(): Int = null!!
    override fun previousIndex(): Int = null!!
}

fun expectUoe(block: () -> Any) {
    try {
        block()
        throw AssertionError()
    } catch (e: UnsupportedOperationException) {
    }
}

fun box(): String {
    val list = MyListIterator<String>() as java.util.ListIterator<String>

    expectUoe { list.set("") }
    expectUoe { list.add("") }

    return "OK"
}
