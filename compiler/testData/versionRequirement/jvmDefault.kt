package test

interface Base {
    @JvmDefault
    fun foo() {}
}

interface Derived : Base
