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
    A.<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>f<!>()+A.<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>g<!>
    <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B<!>()
    <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>bar<!>()
    <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>baz<!>()

    println(::<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>bar<!>.name)
    println(::<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>baz<!>.name)
    println(<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>A<!>::<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>f<!>.name)

    B.<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>Nested<!>()

    boo<<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B?<!>>(null)
    <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>boo<!>(null as B?)
    boo<<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B.Nested?<!>>(null)

    println(<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B::class<!>)
    println(<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B.Nested::class<!>)

    val x: Any = 1
    println(x is B)
}

external class DerivedB : <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B<!>

inline fun <reified T> boo(x: T) {
    println("${T::class.simpleName}: $x")
}
