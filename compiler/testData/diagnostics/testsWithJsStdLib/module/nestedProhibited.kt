@file:JsModule("foo")
package foo

@JsModule("A")
@native class <!NESTED_JS_MODULE_PROHIBITED!>A<!>

@JsModule("B")
@native <!NESTED_JS_MODULE_PROHIBITED!>object B<!>

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("foo")
@native fun foo()<!> = 23

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("bar")
@native val bar<!> = 42

<!NESTED_JS_MODULE_PROHIBITED!>@JsNonModule
@native val baz<!> = 99