// FIR_IDENTICAL
// FIR_COMPARISON
fun foo(): String {
    ret<caret>
}

// INVOCATION_COUNT: 1
// ABSENT: "return null"
// ABSENT: "return true"
// ABSENT: "return false"
