class T

object Extensions {
    fun T.foo() {}
}

fun T.usage() {
    f<caret>
}

// ELEMENT: foo