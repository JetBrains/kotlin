// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZArray(val storage: IntArray) : Collection<Z<Int>> {
    override val size: Int
        get() = storage.size

    override fun contains(element: Z<Int>): Boolean {
        return storage.contains(element.x)
    }

    override fun containsAll(elements: Collection<Z<Int>>): Boolean {
        return elements.all { contains(it) }
    }

    override fun isEmpty(): Boolean {
        return storage.isEmpty()
    }

    private class ZArrayIterator(val storage: IntArray): Iterator<Z<Int>> {
        var index = 0

        override fun hasNext(): Boolean = index < storage.size

        override fun next(): Z<Int> = Z(storage[index++])
    }

    override fun iterator(): Iterator<Z<Int>> = ZArrayIterator(storage)
}


fun box(): String {
    val zs = ZArray(IntArray(5))

    val testSize = zs.size
    if (testSize != 5) return "Failed: testSize=$testSize"

    val testContains = zs.contains(object {} as Any)
    if (testContains) return "Failed: testContains=$testContains"

    return "OK"
}
