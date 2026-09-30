// RUN_PIPELINE_TILL: FRONTEND

// FILE: declarationQualifier.kt
package declarationQualifier

@JsModule("m")
@JsQualifier("a.b")
external fun foo(): Int

@JsModule("m")
@JsQualifier("a.b")
external val bar: Int

// Unlike on Wasm, a qualifier does not help: the qualifier is ignored and the module is imported directly.
<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("m")
@JsQualifier("a.b")
external var baz: Int<!>

@JsModule("m")
@JsQualifier("a.b")
external class A

@JsModule("m")
@JsQualifier("a.b")
external object O

// FILE: fileQualifier.kt
@file:JsQualifier("q")
package fileQualifier

@JsModule("m")
external fun foo(): Int

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("m")
external var baz: Int<!>

@JsModule("m")
external class A

@JsModule("m")
external object O
