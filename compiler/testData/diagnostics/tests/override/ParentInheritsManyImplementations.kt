package d

interface A {
    fun foo() = 1
}

interface B {
    fun foo() = 2
}

open <!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class C<!> : A, B {}

interface E {
    fun foo(): Int
}

class D : C() {}
