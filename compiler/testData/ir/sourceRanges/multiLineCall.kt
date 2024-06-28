// WITH_STDLIB

fun foo() {
    test().
    test().
    fail()
}

inline fun test(): String {
    return "123"
}

inline fun String.test(): String {
    return "123"
}

fun String.fail(): String {
    throw AssertionError("fail")
}

fun bar() {
    "123".
    test
}

val String.test: String
    get() = this

fun baz() {
    E.
    SINGLE

    O::
    foo

    ""::
    fail
}

enum class E {
    SINGLE
}

object O {
    fun foo() {}
}
