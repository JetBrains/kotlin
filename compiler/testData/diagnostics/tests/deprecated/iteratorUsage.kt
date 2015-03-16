class Iter {
    deprecated("text")
    fun iterator() : IterIterator = throw Exception()

    class IterIterator {
        fun hasNext(): Boolean = throw UnsupportedOperationException()
        fun next(): String = throw UnsupportedOperationException()
    }
}

class Iter2 {
    fun iterator() : Iter2Iterator = throw Exception()
    class Iter2Iterator {
        deprecated("text")
        fun hasNext(): Boolean = throw UnsupportedOperationException()
        deprecated("text")
        fun next(): String = throw UnsupportedOperationException()
    }
}

fun use() {
    for (x in <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Iter()<!>) {}
    for (x in <!DEPRECATED_SYMBOL_WITH_MESSAGE, DEPRECATED_SYMBOL_WITH_MESSAGE!>Iter2()<!>) {}
}