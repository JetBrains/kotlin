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

// ABSENT: A
// EXIST: { lookupString:"B", itemText:"E.B" }
// EXIST: { lookupString:"C", itemText:"E.C" }
// ABSENT:else
