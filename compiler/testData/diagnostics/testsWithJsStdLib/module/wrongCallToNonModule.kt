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
    A.<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>f<!>()+A.<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>g<!>
    <!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B<!>()
    <!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>bar<!>()
}

external class DerivedB : <!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B<!>