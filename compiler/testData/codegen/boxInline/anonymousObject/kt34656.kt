// FILE: 1.kt
package test

interface Foo {
    fun call(): String
}

inline fun f(crossinline g: () -> String) = object: Foo {
    fun <T> foo() = g()
    fun <T> bar() = "K"
    override fun call(): String = foo<String>() + bar<String>()
}

// FILE: 2.kt
import test.*

val x = f { "O" }

fun box() : String {
    return x.call()
}