// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
class A

interface First {
    context(a: A)
    fun foo()

    context(a: A)
    val b: String
}

interface Second {
    fun Any.foo()
    val Any.b: String
}

interface Third {
    fun foo(a: Any)
}

interface IntersectionContextWithExtension : First, Second

interface IntersectionContextWithValue : First, Third

fun usage(a: IntersectionContextWithExtension, b: IntersectionContextWithValue) {
    with(A()) {
        a.foo()
        a.b
    }
    with(a) {
        "".foo()
        "".b
    }
}