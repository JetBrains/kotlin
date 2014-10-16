package h

open class A {
    fun bar() = if (this is B) this.foo() else "fail"
}

class B() : A() {
    fun foo() = "OK"
}

fun box() = B().bar()