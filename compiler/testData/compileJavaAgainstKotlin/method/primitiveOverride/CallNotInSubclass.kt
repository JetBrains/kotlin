package test

interface A<T> {
    fun foo(): T
}

class B : A<Int> {
    override fun foo(): Int = 42
}
