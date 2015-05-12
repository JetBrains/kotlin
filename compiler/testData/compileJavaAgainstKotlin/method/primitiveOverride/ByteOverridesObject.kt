package test

interface A<T> {
    fun foo(): T
}

open class B : A<Byte> {
    override fun foo(): Byte = 42
}

abstract class C : A<Byte>
