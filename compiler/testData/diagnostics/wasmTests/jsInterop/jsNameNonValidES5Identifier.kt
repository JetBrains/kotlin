// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-64144
// OPT_IN: kotlin.js.ExperimentalJsExport

@JsExport
@JsName("test:records/test#swap-tuple")
fun wasmExportSwapTuple(p0: Int, p1: Int): Int = p0 + p1

@JsExport
@JsName("kebab-case-name")
fun kebab(): Int = 1

@JsExport
@JsName("with space")
fun withSpace(): Int = 2

@JsExport
@JsName("quote'inside")
fun quoteInside(): Int = 3

@JsName("non-exported-name")
fun notExported(): Int = 4

external class ExternalWithWeirdName {
    @JsName("weird-member")
    fun member(): Int
}
