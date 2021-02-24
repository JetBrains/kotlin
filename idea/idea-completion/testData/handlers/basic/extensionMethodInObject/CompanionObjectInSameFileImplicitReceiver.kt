class T {
    companion object {
        fun T.foo() {}
    }
}

fun T.usage() {
    f<caret>
}

// ELEMENT: foo