package test

interface A<T> {
    fun foo(): T
}

open class B : A<Int> {
    override fun foo(): Int = 42
}

abstract class C : A<Int>
