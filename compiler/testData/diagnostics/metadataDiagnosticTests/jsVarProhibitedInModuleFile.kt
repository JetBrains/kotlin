// METADATA_TARGET_PLATFORMS: JS, WasmJs
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// ISSUE: KT-88343

// FILE: moduleFile.kt
@file:JsModule("moduleFile")
package moduleFile

// Reported by both the JS and the Wasm checkers, since the declaration is a non-writable import on both platforms.
external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING, JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> moduleVar: Int

external val moduleVal: Int

// FILE: moduleWithQualifierFile.kt
@file:JsModule("moduleWithQualifierFile")
@file:JsQualifier("a.b")
package moduleWithQualifierFile

// Reported by the JS checker only: on Wasm, a qualified declaration is writable.
external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> qualifiedFileVar: Int
