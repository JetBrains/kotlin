class MyCollection<T>: Collection<T> {
    override fun size(): Int = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: Any?): Boolean = false
    override fun iterator(): Iterator<T> = throw UnsupportedOperationException()
    override fun toArray(): Array<Any?> = throw UnsupportedOperationException()
    override fun <E> toArray(a: Array<out E>): Array<E> = throw UnsupportedOperationException()
    override fun containsAll(c: Collection<Any?>): Boolean = false
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
    val collection = MyCollection<String>() as MutableCollection<String>

    expectUoe { collection.add("") }
    expectUoe { collection.remove("") }
    expectUoe { collection.addAll(collection) }
    expectUoe { collection.removeAll(collection) }
    expectUoe { collection.retainAll(collection) }
    expectUoe { collection.clear() }

    return "OK"
}