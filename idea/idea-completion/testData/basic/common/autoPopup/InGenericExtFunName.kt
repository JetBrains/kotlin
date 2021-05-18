// FIR_COMPARISON
class Outer {
    class Nested
}

fun <T> Outer.<caret>

// INVOCATION_COUNT: 0
// EXIST: Nested
// NOTHING_ELSE