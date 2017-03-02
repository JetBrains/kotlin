package a

interface A<T> {
    fun foo(): T
}

open class C: A<Int> {
    override fun foo(): Int = 42
}