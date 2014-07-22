enum class E {
    A
    B
}

fun f(e1: E, e2: E?, x: Any) {
    if (e1 == <caret>
}

// EXIST: E.A
// EXIST: E.B
// EXIST: { itemText:"e2" }
// ABSENT: { itemText:"!! e2" }
// ABSENT: e1
// ABSENT: x
// ABSENT: null
