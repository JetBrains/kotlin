package test

interface A<T> {
    fun f(t: T)
}

open class B {
    fun f(i: Int) {}
}

class C: B(), A<Int>
