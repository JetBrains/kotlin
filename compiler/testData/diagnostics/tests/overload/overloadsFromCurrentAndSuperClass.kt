// SKIP_TXT
// FIR_IDENTICAL

abstract class A {
    open public fun foo(x: Any) {}
    open public fun foo(x: String) {}
}

class B : A() {
    override fun foo(x: Any) {
        super.foo(x)
    }

    override fun foo(x: String) {
        super.foo(x)
    }
}

fun bar(a: A) {
    if (a is B) {
        a.foo("")
    }
}
