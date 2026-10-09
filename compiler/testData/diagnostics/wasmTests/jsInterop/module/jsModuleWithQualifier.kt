// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop

// FILE: declarationQualifier.kt
package declarationQualifier

@JsModule("m")
@JsQualifier("a.b")
external fun foo(): Int

@JsModule("m")
@JsQualifier("a.b")
external val bar: Int

// A qualified declaration is referenced through its qualifier, so writing to it does not reach the module.
@JsModule("m")
@JsQualifier("a.b")
external var baz: Int

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

@JsModule("m")
external var baz: Int

@JsModule("m")
external class A

@JsModule("m")
external object O
