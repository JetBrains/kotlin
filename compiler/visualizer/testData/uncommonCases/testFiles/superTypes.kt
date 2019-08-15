package org.jetbrains.kotlin.test

abstract class Base<T>(var x: T) {
    fun replace(newValue: T)
}

class Derived(var x: Int): Base<Int>() {
    override fun replace(newValue: Int) {
        x = newValue
    }
}

fun test() {
    Derived(10).replace(20)
}