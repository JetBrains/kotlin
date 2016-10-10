// WITH_RUNTIME

class ComparablePair<T : Comparable<T>>(val first: T, val second: T) : Comparable<ComparablePair<T>> {
    override fun compareTo(other: ComparablePair<T>): Int {
        val result = first.compareTo(other.first)
        return if (result != 0) result else second.compareTo(other.second)
    }
}

fun box(): String {
    assert("a" !in "b".."c")
    assert("b" in "a".."d")

    assert(ComparablePair(2, 2) !in ComparablePair(1, 10)..ComparablePair(2, 1))
    assert(ComparablePair(2, 2) in ComparablePair(2, 0)..ComparablePair(2, 10))

    return "OK"
}
