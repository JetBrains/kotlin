class T {
    companion object {
        fun T.foo() {}
    }
}

fun usage(t: T) {
    t.f<caret>
}

// INVOCATION_COUNT: 2
// ELEMENT: foo