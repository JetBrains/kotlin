// FIR_IDENTICAL
// FIR_COMPARISON
fun foo(p: Int?): Int = p ?: <caret>

// ABSENT: return
