// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop, kotlin.wasm.ExperimentalWasmInterop

// FILE: declarationModule.kt
package declarationModule

import kotlin.wasm.WasmImport

// A declaration with its own module annotation is the default export of the module, so its name is not used.
@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("bar")<!>
external fun foo(): Int

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("w")<!>
external val v: Int

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("B")<!>
external class A

// An object with its own module annotation is the module itself.
@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("P")<!>
external object O

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
external fun qualified(): Int

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("bar")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
external fun both(): Int

// These annotations tell where a function comes from on their own, so the other ones are ignored.
<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsModule("m")<!>
@JsFun("() => 1")
external fun jsFunInModule(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsModule("m")<!>
@WasmImport("a", "b")
external fun wasmImportInModule(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsName("bar")<!>
@JsFun("() => 1")
external fun namedJsFun(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsQualifier("a.b")<!>
@WasmImport("a", "b")
external fun qualifiedWasmImport(): Int

// A member is looked up in its container by name.
@JsModule("m")
external class WithMembers {
    @JsName("g")
    fun f(): Int
}

// FILE: fileModule.kt
@file:JsModule("m")
package fileModule

// A declaration of a module file is looked up in the module by its name and qualifier.
@JsName("bar")
external fun foo(): Int

@JsQualifier("a.b")
external fun qualified(): Int

@JsName("B")
@JsQualifier("a.b")
external class A

// FILE: noModule.kt
package noModule

@JsName("bar")
@JsQualifier("a.b")
external fun foo(): Int
