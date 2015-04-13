class C(val v: Int)

fun foo(): C {
    return <caret>a.b(1)
}

// ELEMENT: C
// CHAR: \t