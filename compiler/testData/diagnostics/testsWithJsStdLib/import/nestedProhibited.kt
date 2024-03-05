// FIR_IDENTICAL
@file:JsImport("foo")
package foo

@JsImport("A")
external class <!NESTED_JS_IMPORT_PROHIBITED!>A<!> {
    class Nested
}

@JsImport("B")
external <!NESTED_JS_IMPORT_PROHIBITED!>object B<!>

<!NESTED_JS_IMPORT_PROHIBITED!>@JsImport("foo")
external fun foo(): Int<!>

<!NESTED_JS_IMPORT_PROHIBITED!>@JsImport("bar")
external val bar: Int<!>