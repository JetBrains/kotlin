package zzz

interface I {
    fun foo(): Int
}

open class A : I {
    override fun foo() = 42
}

open class B : I by A() {
    val x = 117
    val y = "zzz"
}