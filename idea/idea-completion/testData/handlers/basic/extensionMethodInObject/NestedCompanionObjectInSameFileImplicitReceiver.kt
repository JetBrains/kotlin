class T

class A {
    class B {
        companion object {
            fun T.foo() {}
        }
    }
}

fun T.usage() {
    f<caret>
}

// INVOCATION_COUNT: 2
// ELEMENT: foo