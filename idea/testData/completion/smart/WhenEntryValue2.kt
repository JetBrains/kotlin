enum class E {
    A
    B
    C
}

fun foo(e: E) {
    when(e) {
        E.A, <caret>
    }
}

// ABSENT: E.A
// EXIST: E.B
// EXIST: E.C
// ABSENT:else
