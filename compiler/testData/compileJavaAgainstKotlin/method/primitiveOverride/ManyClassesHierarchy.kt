package test

interface A<T> {
    fun foo(): T
}

interface B : A<Int>

abstract class C : B

open class D : C() {
    override fun foo(): Int = 42
}
