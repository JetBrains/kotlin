package test

interface A {
    fun foo(): Any
}

open class B : A {
    override fun foo(): Int = 42
}
