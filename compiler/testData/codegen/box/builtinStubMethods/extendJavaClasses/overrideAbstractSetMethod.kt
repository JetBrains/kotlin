// DONT_TARGET_EXACT_BACKEND: JS
// WITH_STDLIB

class MySet<K, V, E : Map.Entry<K, V>>: AbstractSet<E>() {
    override fun contains(element: E): Boolean { return element.key !== null }

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = false
    override fun containsAll(elements: Collection<E>): Boolean = false

    override fun iterator(): Iterator<E> = TODO("")
}

fun box(): String {
    val h = MySet<Int, Int, Map.Entry<Int, Int>>()
    val c = (object {}).let { h.contains(it as Any?) }
    return if (c) "NOT OK" else "OK"
}
