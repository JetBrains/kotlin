// ISSUE: KT-57192
// FIR_IDENTICAL
// OPT_IN: kotlin.js.ExperimentalJsExport
// Promise<Unit> wrongly raised:
// K1: NON_EXPORTABLE_TYPE
// K1,K2: UPPER_BOUND_VIOLATED
// IGNORE_BACKEND: WASM

import kotlin.js.Promise

@JsExport
fun fooJsNumber(p: Promise<JsNumber>): Promise<JsNumber>? = p

@JsExport
fun fooUnitReturn(): Promise<Unit>? = null

@JsExport
fun fooUnitArgument(p: Promise<Unit>) = p.hashCode()
