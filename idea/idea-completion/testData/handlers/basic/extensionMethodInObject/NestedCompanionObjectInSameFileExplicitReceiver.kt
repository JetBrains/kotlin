class T

class A {
    class B {
        companion object {
            fun T.foo() {}
        }
    }
}

fun usage(t: T) {
    t.f<caret>
}

// INVOCATION_COUNT: 2
// ELEMENT: foo