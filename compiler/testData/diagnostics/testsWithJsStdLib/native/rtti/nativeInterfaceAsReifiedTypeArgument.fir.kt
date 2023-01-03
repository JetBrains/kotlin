// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T> foo(x: T) {
    println(x)
}

external interface I

external class C : I

operator inline fun <reified T> C.plus(other: T) = this

fun bar() {
    foo(C())

    val c: I = C()
    foo(c)
    foo<I>(C())

    C() + c
}
