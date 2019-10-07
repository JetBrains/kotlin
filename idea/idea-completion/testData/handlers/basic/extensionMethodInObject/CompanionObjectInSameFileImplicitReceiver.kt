class T {
    companion object {
        fun T.foo() {}
    }
}

fun T.usage() {
    f<caret>
}

// INVOCATION_COUNT: 2
// ELEMENT: foo