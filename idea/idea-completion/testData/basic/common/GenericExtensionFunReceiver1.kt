// FIR_COMPARISON
fun foo() {}

fun <T> S<caret>

// INVOCATION_COUNT: 1
// EXIST: String
// EXIST: Set
// ABSENT: foo
