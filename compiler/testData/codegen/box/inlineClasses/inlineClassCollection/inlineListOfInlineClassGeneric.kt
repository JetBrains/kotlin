// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZArray(val storage: IntArray) : List<Z<Int>> {
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

    override fun get(index: Int): Z<Int> = Z(storage[index])

    override fun indexOf(element: Z<Int>): Int = storage.indexOf(element.x)

    override fun lastIndexOf(element: Z<Int>): Int = storage.lastIndexOf(element.x)

    override fun listIterator(): ListIterator<Z<Int>> = ZArrayIterator(storage)

    override fun listIterator(index: Int): ListIterator<Z<Int>> = ZArrayIterator(storage, index)

    override fun subList(fromIndex: Int, toIndex: Int): List<Z<Int>> = TODO()

    private class ZArrayIterator(val storage: IntArray, var index: Int = 0): ListIterator<Z<Int>> {
        override fun hasNext(): Boolean = index < storage.size
        override fun next(): Z<Int> = Z(storage[index++])
        override fun nextIndex(): Int = index + 1

        override fun hasPrevious(): Boolean = index > 0
        override fun previous(): Z<Int> = Z(storage[index--])
        override fun previousIndex(): Int = index - 1
    }

    override fun iterator(): Iterator<Z<Int>> = ZArrayIterator(storage)
}


fun box(): String {
    val zs = ZArray(IntArray(5))

    val testElement = object {} as Any
    zs.contains(testElement)
    zs.indexOf(testElement)
    zs.lastIndexOf(testElement)

    return "OK"
}
