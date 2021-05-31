// FIR_COMPARISON
fun foo(): String {
    val s = "$<caret>xxx"
}

// ELEMENT: foo
// CHAR: \t
