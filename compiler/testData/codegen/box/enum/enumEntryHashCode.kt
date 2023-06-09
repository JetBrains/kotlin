// WITH_STDLIB

enum class E {
    A, B
}

fun box() = if (E.A.hashCode() == E.A.ordinal && E.B.hashCode() == E.B.ordinal) "FAIL" else "OK"