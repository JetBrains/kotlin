class Outer {
    class Nested
    inner class Inner
}

fun Outer.foo() {
    <caret>
}

// ABSENT: Nested
// EXIST: { itemText: "Inner", tailText: "()", typeText: "Outer.Inner" }
