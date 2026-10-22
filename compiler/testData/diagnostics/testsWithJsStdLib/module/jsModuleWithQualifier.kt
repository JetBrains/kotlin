// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: declarationQualifier.kt
package declarationQualifier

@JsModule("m")
@JsQualifier("a.b")
external fun foo(): Int

@JsModule("m")
@JsQualifier("a.b")
external val bar: Int

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

<!JS_MODULE_PROHIBITED_IN_FILE_WITH_JS_QUALIFIER!>@JsModule("m")<!>
external fun foo(): Int

<!JS_MODULE_PROHIBITED_IN_FILE_WITH_JS_QUALIFIER!>@JsModule("m")<!>
external val bar: Int

<!JS_MODULE_PROHIBITED_ON_VAR!><!JS_MODULE_PROHIBITED_IN_FILE_WITH_JS_QUALIFIER!>@JsModule("m")<!>
external var baz: Int<!>

<!JS_MODULE_PROHIBITED_IN_FILE_WITH_JS_QUALIFIER!>@JsModule("m")<!>
external class A

<!JS_MODULE_PROHIBITED_IN_FILE_WITH_JS_QUALIFIER!>@JsModule("m")<!>
external object O
