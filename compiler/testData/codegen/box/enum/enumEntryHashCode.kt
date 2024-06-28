// WITH_STDLIB

enum class E {
    A, B
}

// hashCode shouldn't always return ordinal: KT-59223
fun box() = if (E.A.hashCode() == E.A.ordinal && E.B.hashCode() == E.B.ordinal) "FAIL" else "OK"