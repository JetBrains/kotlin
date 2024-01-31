// WITH_STDLIB

abstract class A : AbstractMutableList<Int>()

class B : A() {
    override fun iterator(): MutableIterator<Int> = mutableListOf(0).iterator()
    override val size = 0
    override fun add(index: Int, element: Int) {}
    override fun get(index: Int) = index
    override fun removeAt(index: Int) = index
    override fun set(index: Int, element: Int) = index
}

fun box(): String {
    val b = B()
    return if (b.remove(0)) "OK" else "Fail"
}
