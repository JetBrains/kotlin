enum class E {
    A
    B
    C
}

fun foo(e: E) {
    when(e) {
        E.A -> x()
        E.<caret>
    }
}

// ABSENT: A
// EXIST: B
// EXIST: C
// ABSENT: else
