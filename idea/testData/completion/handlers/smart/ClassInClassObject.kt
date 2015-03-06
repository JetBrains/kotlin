class A {
    default object {
        class Inner {}
    }
}

val v: A.Default.Inner = <caret>

// ELEMENT: Inner
