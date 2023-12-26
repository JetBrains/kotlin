// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-62550

class A<U : Number, V : U, W : V> : Set<W> {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: W): Boolean = false
    override fun iterator(): Iterator<W> = emptySet<W>().iterator()
    override fun containsAll(c: Collection<W>): Boolean = c.isEmpty()
}

fun expectUoe(block: () -> Any) {
    try {
        block()
        throw AssertionError()
    } catch (e: UnsupportedOperationException) {
    }
}

fun box(): String {
    val a = A<Int, Int, Int>() as java.util.Set<Int>

    a.iterator()

    expectUoe { a.add(42) }
    expectUoe { a.remove(42) }
    expectUoe { a.addAll(a) }
    expectUoe { a.removeAll(a) }
    expectUoe { a.retainAll(a) }
    expectUoe { a.clear() }

    return "OK"
}
