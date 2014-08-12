package d

trait A {
    fun foo() = 1
}

trait B {
    fun foo() = 2
}

open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C<!> : A, B {}

trait E {
    fun foo(): Int
}

class D : C() {}
