// MODULE: m1
// FILE: a.kt
package foo

import kotlin.js.*

@JsModule("A")
external object A {
    fun f(): Int

    val g: Int
}

@JsNonModule
external open class B {
    fun foo(): Int
}

// MODULE: m2(m1)
// MODULE_KIND: UMD
// FILE: c.kt
package bar

import foo.*

fun box() {
    A.f()+A.g
    B()
}
