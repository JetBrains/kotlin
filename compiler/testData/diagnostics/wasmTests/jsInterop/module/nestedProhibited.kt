// FIR_IDENTICAL
@file:JsModule("foo")
package foo

@JsModule("A")
external class <!NESTED_JS_MODULE_PROHIBITED!>A<!> {
    class Nested
}

@JsModule("B")
external <!NESTED_JS_MODULE_PROHIBITED!>object B<!>

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("foo")
external fun foo(): Int<!>

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("bar")
external val bar: Int<!>