enum class E {
    A,
    B
}

fun f(e1: E, e2: E?, x: Any) {
    if (e1 != <caret>
}

// EXIST: { lookupString:"A", itemText:"E.A" }
// EXIST: { lookupString:"B", itemText:"E.B" }
// EXIST: e2
// ABSENT: e1
// ABSENT: x
