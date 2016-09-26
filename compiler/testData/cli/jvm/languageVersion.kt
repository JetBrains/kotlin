package test

sealed class Base

class Derived : Base()

fun test() {
    ""::class.isInstance(42)
}
