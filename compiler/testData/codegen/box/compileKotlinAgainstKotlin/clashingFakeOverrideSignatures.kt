// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND: NATIVE
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION
// Reason: KT-42020

// MODULE: lib
// FILE: a.kt
package a

open class Base<T> {
    fun foo(x: T) = "x:$x"
    fun foo(y: String) = "y:$y"
}

// MODULE: main(lib)
// FILE: b.kt
import a.Base

open class Derived : Base<String>()

fun box(): String {
    val b = Base<String>()
    val test1 = b.foo(x = "O") + b.foo(y = "K")
    if (test1 != "x:Oy:K")
        throw Exception("test1: $test1")

    val d = Derived()
    val test2 = d.foo(x = "O") + d.foo(y = "K")
    if (test2 != "x:Oy:K")
        throw Exception("test2: $test2")

    val bd: Base<String> = Derived()
    val test4 = bd.foo(x = "O") + bd.foo(y = "K")
    if (test4 != "x:Oy:K")
        throw Exception("test4: $test4")

    return "OK"
}
