class T

object Extensions {
    fun T.foo() {}
}

fun T.usage() {
    f<caret>
}

// INVOCATION_COUNT: 2
// ELEMENT: foo