// MODULE: m1
// FILE: a.kt
package foo

import kotlin.js.*

@JsModule("A")
external object A {
    fun f(): Int

    val g: Int
}

@JsModule("B")
external open class B {
    fun foo(): Int

    class Nested
}

@JsModule("bar")
external fun bar(): Unit

// MODULE: m2(m1)
// FILE: b.kt
// TODO: it's hard to test @JsNonModule on file from an external module
@file:JsModule("foo")
package foo

external fun baz(): Unit

// FILE: c.kt
package bar

import foo.*

fun box() {
    A.f()+A.g
    B()
    bar()
    baz()

    println(::bar.name)
    println(::baz.name)
    println(A::f.name)

    B.Nested()

    boo<B?>(null)
    boo(null as B?)
    boo<B.Nested?>(null)

    println(B::class)
    println(B.Nested::class)
}

external class DerivedB : B

inline fun <reified T> boo(x: T) {
    println("${T::class.simpleName}: $x")
}
