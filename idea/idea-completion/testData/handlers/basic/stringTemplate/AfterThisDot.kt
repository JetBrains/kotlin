// FIR_COMPARISON
fun String.foo() {
    val s = "$this.<caret>"
}

// ELEMENT: equals
// TAIL_TEXT: "(other: Any?)"