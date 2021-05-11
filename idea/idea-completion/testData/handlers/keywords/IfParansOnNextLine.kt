// FIR_COMPARISON
fun other(a: Any) {
    i<caret>
    (a as Int).let { println(a + 12) }
}

// ELEMENT: if