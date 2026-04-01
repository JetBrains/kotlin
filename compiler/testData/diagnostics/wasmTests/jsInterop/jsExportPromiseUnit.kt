// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57192
// OPT_IN: kotlin.js.ExperimentalJsExport
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop

import kotlin.js.Promise

@JsExport
fun fooJsNumber(p: Promise<JsNumber>): Promise<JsNumber>? = p

<!NON_EXPORTABLE_TYPE!>@JsExport
fun fooUnitReturn(): Promise<<!UPPER_BOUND_VIOLATED!>Unit<!>>?<!> = null

@JsExport
fun fooUnitArgument(<!NON_EXPORTABLE_TYPE!>p: Promise<<!UPPER_BOUND_VIOLATED!>Unit<!>><!>) = p.hashCode()
