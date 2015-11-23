enum class E { A, B }

fun foo(e: E, something: Any?): Int {
    if (something != null) return 0

    return when (e) {
        E.A -> 1
        E.B -> 2
        <!DEBUG_INFO_CONSTANT!>something<!> -> 3
    }
}
