class Iter {
    @Deprecated("text")
    fun iterator() : IterIterator = throw Exception()

    class IterIterator {
        fun hasNext(): Boolean = throw UnsupportedOperationException()
        fun next(): String = throw UnsupportedOperationException()
    }
}

class Iter2 {
    fun iterator() : Iter2Iterator = throw Exception()
    class Iter2Iterator {
        @Deprecated("text")
        fun hasNext(): Boolean = throw UnsupportedOperationException()
        @Deprecated("text")
        fun next(): String = throw UnsupportedOperationException()
    }
}

fun use() {
    for (x in <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Iter()<!>) {}
    for (x in <!DEPRECATED_SYMBOL_WITH_MESSAGE, DEPRECATED_SYMBOL_WITH_MESSAGE!>Iter2()<!>) {}
}