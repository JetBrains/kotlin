// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop

// FILE: declarationQualifier.kt
package declarationQualifier

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
external fun foo(): Int

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
external val bar: Int

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
external var baz: Int<!>

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
external class A

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
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
