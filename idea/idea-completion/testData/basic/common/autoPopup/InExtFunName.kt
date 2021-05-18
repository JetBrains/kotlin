// FIR_COMPARISON
class Outer {
    class Nested
}

fun Outer.<caret>

// INVOCATION_COUNT: 0
// EXIST: Nested
// NOTHING_ELSE