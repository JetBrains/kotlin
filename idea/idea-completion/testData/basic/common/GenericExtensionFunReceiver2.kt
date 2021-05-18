// FIR_COMPARISON
class Foo{}

fun foo() {}

fun <T> <caret>

// INVOCATION_COUNT: 1
// EXIST: Foo
// EXIST: String
// EXIST: Set
// ABSENT: foo
