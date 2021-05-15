// FIR_COMPARISON
class AAA {
    class Nested
    inner class Inner
}

fun a(a: AAA.<caret>) {
}

// EXIST: Nested
// EXIST: Inner
// NOTHING_ELSE