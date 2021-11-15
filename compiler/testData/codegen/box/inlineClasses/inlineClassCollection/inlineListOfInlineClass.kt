// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class ZArray(val storage: IntArray) : List<Z> {
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

    override fun get(index: Int): Z = Z(storage[index])

    override fun indexOf(element: Z): Int = storage.indexOf(element.x)

    override fun lastIndexOf(element: Z): Int = storage.lastIndexOf(element.x)

    override fun listIterator(): ListIterator<Z> = ZArrayIterator(storage)

    override fun listIterator(index: Int): ListIterator<Z> = ZArrayIterator(storage, index)

    override fun subList(fromIndex: Int, toIndex: Int): List<Z> = TODO()

    private class ZArrayIterator(val storage: IntArray, var index: Int = 0): ListIterator<Z> {
        override fun hasNext(): Boolean = index < storage.size
        override fun next(): Z = Z(storage[index++])
        override fun nextIndex(): Int = index + 1

        override fun hasPrevious(): Boolean = index > 0
        override fun previous(): Z = Z(storage[index--])
        override fun previousIndex(): Int = index - 1
    }

    override fun iterator(): Iterator<Z> = ZArrayIterator(storage)
}


fun box(): String {
    val zs = ZArray(IntArray(5))

    val testElement = object {} as Any
    zs.contains(testElement)
    zs.indexOf(testElement)
    zs.lastIndexOf(testElement)

    return "OK"
}
