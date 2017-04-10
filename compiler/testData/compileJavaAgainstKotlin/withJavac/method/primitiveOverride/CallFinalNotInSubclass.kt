package test

interface A<T> {
    fun foo(): T
}

class B : A<Int> {
    override final fun foo(): Int = 42
}
