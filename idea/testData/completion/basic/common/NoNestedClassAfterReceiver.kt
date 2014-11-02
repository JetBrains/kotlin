class C {
    class Nested
    inner class Inner
}

fun foo(c: C) {
    c.<caret>
}

// ABSENT: Nested
// ABSENT: Inner
