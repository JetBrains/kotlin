// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

open class X {
    open val x: CharSequence = "F"
}

class A(override val x: String) : X() {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = x == other.x
}

fun foo(): Any = A("O")

fun box(): String {
    val a = foo()
    return (if (A("O") == a) a.x else "") + (if (A("F") != X()) "K" else "")
}
