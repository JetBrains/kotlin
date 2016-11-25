@file:JsModule("foo")
package foo

@JsModule("A")
external class <!NESTED_JS_MODULE_PROHIBITED!>A<!>

@JsModule("B")
external <!NESTED_JS_MODULE_PROHIBITED!>object B<!>

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("foo")
external fun foo()<!> = 23

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("bar")
external val bar<!> = 42

<!NESTED_JS_MODULE_PROHIBITED!>@JsNonModule
external val baz<!> = 99