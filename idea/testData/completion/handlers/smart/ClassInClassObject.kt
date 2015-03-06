class A {
    class object {
        class Inner {}
    }
}

val v: A.Default.Inner = <caret>

// ELEMENT: Inner
