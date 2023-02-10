// SKIP_TXT
// FIR_IDENTICAL

abstract class A {
    open public fun foo(x: Any): Any = x
    open public fun foo(x: String): String = x
}

class B : A() {
    override fun foo(x: Any): Any = x
    override fun foo(x: String): String = x
}

fun bar(a: A) {
    if (a is B) {
        a.foo("").length
    }
}
