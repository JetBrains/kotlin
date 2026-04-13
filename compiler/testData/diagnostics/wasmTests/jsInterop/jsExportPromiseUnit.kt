// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57192
// OPT_IN: kotlin.js.ExperimentalJsExport
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop

import kotlin.js.Promise

@JsExport
fun fooJsNumber(p: Promise<JsNumber>): Promise<JsNumber>? = p

@JsExport
fun fooUnitReturn(): Promise<<!UPPER_BOUND_VIOLATED!>Unit<!>>? = null

@JsExport
fun fooUnitArgument(p: Promise<<!UPPER_BOUND_VIOLATED!>Unit<!>>) = p.hashCode()
