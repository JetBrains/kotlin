// FIR_IDENTICAL
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTICS_MESSAGES

typealias A = Int

@Suppress("UNUSED_PARAMETER")
@JsExport
fun test(<!NON_EXPORTABLE_TYPE("parameter; A")!>x: A<!>) {}
