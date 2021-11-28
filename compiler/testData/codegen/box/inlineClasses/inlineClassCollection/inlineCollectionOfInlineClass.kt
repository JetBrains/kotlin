// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class ZArray(val storage: IntArray) : Collection<Z> {
    override val size: Int
        get() = storage.size

    override fun contains(element: Z): Boolean {
        return storage.contains(element.x)
    }

    override fun containsAll(elements: Collection<Z>): Boolean {
        return elements.all { contains(it) }
    }

    override fun isEmpty(): Boolean {
        return storage.isEmpty()
    }

    private class ZArrayIterator(val storage: IntArray): Iterator<Z> {
        var index = 0

        override fun hasNext(): Boolean = index < storage.size

        override fun next(): Z = Z(storage[index++])
    }

    override fun iterator(): Iterator<Z> = ZArrayIterator(storage)
}


fun box(): String {
    val zs = ZArray(IntArray(5))

    val testSize = zs.size
    if (testSize != 5) return "Failed: testSize=$testSize"

    val testContains = zs.contains(object {} as Any)
    if (testContains) return "Failed: testContains=$testContains"

    return "OK"
}
