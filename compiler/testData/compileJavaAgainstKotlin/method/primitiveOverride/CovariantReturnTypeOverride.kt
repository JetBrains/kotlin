package test

trait A {
    fun foo(): Any
}

open class B : A {
    override fun foo(): Int = 42
}
