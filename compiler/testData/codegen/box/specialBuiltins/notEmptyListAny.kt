// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS

private object NotEmptyList : MutableList<Any> {
    override fun contains(element: Any): Boolean = true
    override fun indexOf(element: Any): Int = 0
    override fun lastIndexOf(element: Any): Int = 0
    override fun remove(element: Any): Boolean = true

    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun containsAll(elements: Collection<Any>): Boolean = elements.isEmpty()
    override fun isEmpty(): Boolean = throw UnsupportedOperationException()
    override fun get(index: Int): Any = throw UnsupportedOperationException()
    override fun add(element: Any): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<Any>): Boolean = throw UnsupportedOperationException()
    override fun addAll(index: Int, elements: Collection<Any>): Boolean = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<Any>): Boolean = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<Any>): Boolean = throw UnsupportedOperationException()
    override fun clear(): Unit = throw UnsupportedOperationException()
    override fun set(index: Int, element: Any): Any = throw UnsupportedOperationException()
    override fun add(index: Int, element: Any): Unit = throw UnsupportedOperationException()
    override fun removeAt(index: Int): Any = throw UnsupportedOperationException()
    override fun listIterator(): MutableListIterator<Any> = throw UnsupportedOperationException()
    override fun listIterator(index: Int): MutableListIterator<Any> = throw UnsupportedOperationException()
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Any> = throw UnsupportedOperationException()
    override fun iterator(): MutableIterator<Any> = throw UnsupportedOperationException()
}

fun box(): String {
    val n = NotEmptyList as MutableList<Any?>

    if (n.contains(null)) return "fail 1"
    if (n.indexOf(null) != -1) return "fail 2"
    if (n.lastIndexOf(null) != -1) return "fail 3"

    if (!n.contains("")) return "fail 3"
    if (n.indexOf("") != 0) return "fail 4"
    if (n.lastIndexOf("") != 0) return "fail 5"

    if (n.remove(null)) return "fail 6"
    if (!n.remove("")) return "fail 7"

    return "OK"
}
