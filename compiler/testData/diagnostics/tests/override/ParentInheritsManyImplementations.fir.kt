package d

interface A {
    fun foo() = 1
}

interface B {
    fun foo() = 2
}

open class C : A, B {}

interface E {
    fun foo(): Int
}

class D : C() {}
