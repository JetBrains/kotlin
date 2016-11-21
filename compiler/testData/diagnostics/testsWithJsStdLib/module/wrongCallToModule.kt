// MODULE: m1
// FILE: a.kt
package foo

import kotlin.js.*

@JsModule("A")
@native object A {
    fun f(): Int

    val g: Int
}

@JsModule("B")
@native open class B {
    fun foo(): Int
}

@JsModule("bar")
@native fun bar(): Unit

// MODULE: m2(m1)
// FILE: b.kt
// TODO: it's hard to test @JsNonModule on file from an external module
@file:JsModule("foo")
package foo

@native fun baz(): Unit

// FILE: c.kt
package bar

import foo.*

fun box() {
    A.<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>f<!>()+A.<!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>g<!>
    <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B<!>()
    <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>bar<!>()
    <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>baz<!>()
}

@native class DerivedB : <!CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM!>B<!>()