// FIR_IDENTICAL
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
    A.<!CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE!>f<!>()+A.<!CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE!>g<!>
    <!CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE!>B<!>()
}