// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop, kotlin.wasm.ExperimentalWasmInterop, kotlin.js.ExperimentalJsExport

// FILE: exportAnnotations.kt
// Every pair and triple of annotations telling how a declaration is exported.
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsExport
@JsExport.Ignore
fun jsExport_JsExportIgnore(): Int = 1

@JsExport
@JsName("n_jsExport_JsName")
fun jsExport_JsName(): Int = 1

<!JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION!>@JsExport
@WasmExport("e_jsExport_WasmExport")
fun jsExport_WasmExport(): Int = 1<!>

@JsExport.Ignore
@JsName("n_jsExportIgnore_JsName")
fun jsExportIgnore_JsName(): Int = 1

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Ignore<!>
@WasmExport("e_jsExportIgnore_WasmExport")
fun jsExportIgnore_WasmExport(): Int = 1

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsName("n_jsName_WasmExport")<!>
@WasmExport("e_jsName_WasmExport")
fun jsName_WasmExport(): Int = 1

@JsExport
@JsExport.Ignore
@JsName("n_jsExport_JsExportIgnore_JsName")
fun jsExport_JsExportIgnore_JsName(): Int = 1

<!JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION!>@JsExport
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Ignore<!>
@WasmExport("e_jsExport_JsExportIgnore_WasmExport")
fun jsExport_JsExportIgnore_WasmExport(): Int = 1<!>

<!JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION!>@JsExport
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsName("n_jsExport_JsName_WasmExport")<!>
@WasmExport("e_jsExport_JsName_WasmExport")
fun jsExport_JsName_WasmExport(): Int = 1<!>

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Ignore<!>
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsName("n_jsExportIgnore_JsName_WasmExport")<!>
@WasmExport("e_jsExportIgnore_JsName_WasmExport")
fun jsExportIgnore_JsName_WasmExport(): Int = 1

// FILE: jsExport_JsExportDefault.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsExport
@JsExport.Default
fun jsExport_JsExportDefault(): Int = 1

// FILE: jsExportIgnore_JsExportDefault.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsExport.Ignore
@JsExport.Default
fun jsExportIgnore_JsExportDefault(): Int = 1

// FILE: jsExportDefault_JsName.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsExport.Default
@JsName("n_jsExportDefault_JsName")
fun jsExportDefault_JsName(): Int = 1

// FILE: jsExportDefault_WasmExport.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Default<!>
@WasmExport("e_jsExportDefault_WasmExport")
fun jsExportDefault_WasmExport(): Int = 1

// FILE: jsExport_JsExportIgnore_JsExportDefault.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsExport
@JsExport.Ignore
@JsExport.Default
fun jsExport_JsExportIgnore_JsExportDefault(): Int = 1

// FILE: jsExport_JsExportDefault_JsName.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsExport
@JsExport.Default
@JsName("n_jsExport_JsExportDefault_JsName")
fun jsExport_JsExportDefault_JsName(): Int = 1

// FILE: jsExport_JsExportDefault_WasmExport.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

<!JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION!>@JsExport
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Default<!>
@WasmExport("e_jsExport_JsExportDefault_WasmExport")
fun jsExport_JsExportDefault_WasmExport(): Int = 1<!>

// FILE: jsExportIgnore_JsExportDefault_JsName.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

@JsExport.Ignore
@JsExport.Default
@JsName("n_jsExportIgnore_JsExportDefault_JsName")
fun jsExportIgnore_JsExportDefault_JsName(): Int = 1

// FILE: jsExportIgnore_JsExportDefault_WasmExport.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Ignore<!>
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Default<!>
@WasmExport("e_jsExportIgnore_JsExportDefault_WasmExport")
fun jsExportIgnore_JsExportDefault_WasmExport(): Int = 1

// FILE: jsExportDefault_JsName_WasmExport.kt
package exportAnnotations

import kotlin.wasm.WasmImport
import kotlin.wasm.WasmExport

<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsExport.Default<!>
<!INCOMPATIBLE_ANNOTATION_WITH_WASM_EXPORT!>@JsName("n_jsExportDefault_JsName_WasmExport")<!>
@WasmExport("e_jsExportDefault_JsName_WasmExport")
fun jsExportDefault_JsName_WasmExport(): Int = 1
