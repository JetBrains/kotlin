class Iter {
    @Deprecated("text")
    operator fun iterator() : IterIterator = throw Exception()

    class IterIterator {
        operator fun hasNext(): Boolean = throw UnsupportedOperationException()
        operator fun next(): String = throw UnsupportedOperationException()
    }
}

class Iter2 {
    operator fun iterator() : Iter2Iterator = throw Exception()
    class Iter2Iterator {
        @Deprecated("text")
        operator fun hasNext(): Boolean = throw UnsupportedOperationException()
        @Deprecated("text")
        operator fun next(): String = throw UnsupportedOperationException()
    }
}

fun use() {
    for (x in <!DEPRECATION!>Iter<!>()) {}
    for (x in <!DEPRECATION, DEPRECATION!>Iter2<!>()) {}
}
