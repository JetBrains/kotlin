// FIR_IDENTICAL
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
    A.<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>f<!>()+A.<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>g<!>
    <!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B<!>()
    <!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>bar<!>()
    B.<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>Nested<!>()

    println(::<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>bar<!>.name)
    println(<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>A<!>::<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>f<!>.name)

    boo<<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B?<!>>(null)
    <!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>boo<!>(null as B?)
    boo<<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B.Nested?<!>>(null)

    println(<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B::class<!>)
    println(<!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B.Nested::class<!>)

    val x: Any = 1
    println(x is B)
}

external class DerivedB : <!CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM!>B<!>

inline fun <reified T> boo(x: T) {
    println("${T::class.simpleName}: $x")
}
