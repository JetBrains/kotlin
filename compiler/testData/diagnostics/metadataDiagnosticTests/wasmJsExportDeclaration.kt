// METADATA_TARGET_PLATFORMS: JS, WasmJs
// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.js.ExperimentalJsExport
// WITH_STDLIB
// ISSUE: KT-88727

// MODULE: lib-common
// FILE: lib.kt
import kotlin.js.JsExport

<!WRONG_EXPORTED_DECLARATION!>@JsExport
fun libWithVararg(vararg xs: Int): Int<!> = xs.size

// MODULE: app-common(lib-common)
// FILE: app.kt
import kotlin.js.JsExport
import kotlin.js.JsName

class C

@JsExport
fun valid(a: Int, b: Int): Int = a + b

// Kinds that Kotlin/Wasm can't export.
<!WRONG_EXPORTED_DECLARATION!>@JsExport
fun withVararg(vararg xs: Int): Int<!> = xs.size

<!WRONG_EXPORTED_DECLARATION!>@JsExport
fun C.extension(): Int<!> = 0

<!WRONG_EXPORTED_DECLARATION!>@JsExport
suspend fun suspendFun(): Int<!> = 0

<!WRONG_EXPORTED_DECLARATION!>@JsExport
inline fun <reified T> reifiedFun(): Int<!> = 0

<!WRONG_EXPORTED_DECLARATION!>@JsExport
external fun externalFun(): Int<!>

// Names that can't be used for a Kotlin/Wasm JS export.
@JsExport
@JsName(<!RESERVED_WORD_AS_JS_EXPORT_NAME!>"delete"<!>)
fun reservedWord(): Int = 0

@JsExport
@JsName(<!INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME!>"memory"<!>)
fun internalExportName(): Int = 0

// Using an exported function of another module doesn't report anything.
fun callsLib(): Int = libWithVararg(1, 2)
