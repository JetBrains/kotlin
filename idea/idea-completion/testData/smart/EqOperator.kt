enum class E {
    A,
    B
}

fun f(e1: E, e2: E?, x: Any) {
    if (e1 == <caret>
}

// WITH_ORDER
// EXIST: { itemText:"e2" }
// EXIST: { lookupString:"A", itemText:"E.A", typeText:"E" }
// EXIST: { lookupString:"B", itemText:"E.B", typeText:"E" }
// ABSENT: null
// ABSENT: { itemText:"!! e2" }
// ABSENT: e1
// ABSENT: x
