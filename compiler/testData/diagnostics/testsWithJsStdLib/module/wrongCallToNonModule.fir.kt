// MODULE: m1
// FILE: a.kt
package foo

import kotlin.js.*

@JsNonModule
external object A {
    fun f(): Int

    val g: Int
}

@JsNonModule
external open class B {
    fun foo(): Int

    class Nested
}

@JsNonModule
external fun bar(): Unit

// MODULE: m2(m1)
// MODULE_KIND: AMD
// TODO: it's hard to test @JsNonModule on file from an external module
// FILE: c.kt
package bar

import foo.*

fun box() {
    A.f()+A.g
    B()
    bar()
    B.Nested()

    println(::bar.name)
    println(A::f.name)

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
