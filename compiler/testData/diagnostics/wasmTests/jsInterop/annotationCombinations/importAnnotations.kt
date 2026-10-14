// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop, kotlin.wasm.ExperimentalWasmInterop, kotlin.js.ExperimentalJsExport

// FILE: importAnnotations.kt
// Every pair and triple of annotations telling where an external declaration comes from.
package importAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
external fun jsModule_JsQualifier(): Int

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("n_jsModule_JsName")<!>
external fun jsModule_JsName(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsModule("m")<!>
@JsFun("() => 1")
external fun jsModule_JsFun(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsModule("m")<!>
@WasmImport("a", "i_jsModule_WasmImport")
external fun jsModule_WasmImport(): Int

@JsQualifier("a.b")
@JsName("n_jsQualifier_JsName")
external fun jsQualifier_JsName(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsQualifier("a.b")<!>
@JsFun("() => 1")
external fun jsQualifier_JsFun(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsQualifier("a.b")<!>
@WasmImport("a", "i_jsQualifier_WasmImport")
external fun jsQualifier_WasmImport(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsName("n_jsName_JsFun")<!>
@JsFun("() => 1")
external fun jsName_JsFun(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsName("n_jsName_WasmImport")<!>
@WasmImport("a", "i_jsName_WasmImport")
external fun jsName_WasmImport(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsFun("() => 1")<!>
@WasmImport("a", "i_jsFun_WasmImport")
external fun jsFun_WasmImport(): Int

@JsModule("m")
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("n_jsModule_JsQualifier_JsName")<!>
external fun jsModule_JsQualifier_JsName(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsModule("m")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN, INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsQualifier("a.b")<!>
@JsFun("() => 1")
external fun jsModule_JsQualifier_JsFun(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsModule("m")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE, INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsQualifier("a.b")<!>
@WasmImport("a", "i_jsModule_JsQualifier_WasmImport")
external fun jsModule_JsQualifier_WasmImport(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsModule("m")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN, INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE!>@JsName("n_jsModule_JsName_JsFun")<!>
@JsFun("() => 1")
external fun jsModule_JsName_JsFun(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsModule("m")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_JS_MODULE, INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsName("n_jsModule_JsName_WasmImport")<!>
@WasmImport("a", "i_jsModule_JsName_WasmImport")
external fun jsModule_JsName_WasmImport(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN, INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsModule("m")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsFun("() => 1")<!>
@WasmImport("a", "i_jsModule_JsFun_WasmImport")
external fun jsModule_JsFun_WasmImport(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsQualifier("a.b")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN!>@JsName("n_jsQualifier_JsName_JsFun")<!>
@JsFun("() => 1")
external fun jsQualifier_JsName_JsFun(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsQualifier("a.b")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsName("n_jsQualifier_JsName_WasmImport")<!>
@WasmImport("a", "i_jsQualifier_JsName_WasmImport")
external fun jsQualifier_JsName_WasmImport(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN, INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsQualifier("a.b")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsFun("() => 1")<!>
@WasmImport("a", "i_jsQualifier_JsFun_WasmImport")
external fun jsQualifier_JsFun_WasmImport(): Int

<!INCOMPATIBLE_ANNOTATION_WITH_JS_FUN, INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsName("n_jsName_JsFun_WasmImport")<!>
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT!>@JsFun("() => 1")<!>
@WasmImport("a", "i_jsName_JsFun_WasmImport")
external fun jsName_JsFun_WasmImport(): Int
