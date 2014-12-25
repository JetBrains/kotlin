class Outer {
    class Nested
    inner class Inner
}

fun Outer.foo() {
    <caret>
}

// ABSENT: Nested
// ABSENT: Inner
