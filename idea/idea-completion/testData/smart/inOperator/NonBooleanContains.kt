trait X {
    fun contains(s: String): Boolean?
}

fun foo(s: String, x: X) {
    if (s in <caret>)
}

// ABSENT: x
