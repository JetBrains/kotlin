package d

trait A {
    fun foo() = 1
}

trait B {
    fun foo() = 2
}

open class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>C<!> : A, B {}

trait E {
    fun foo(): Int
}

class D : C() {}
