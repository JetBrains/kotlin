enum class E {
    A
    B
    C
}

fun foo(e: E) {
    when(e) {
        <caret>
        else -> x()
    }
}

// EXIST: E.A
// EXIST: E.B
// EXIST: E.C
// ABSENT: else
