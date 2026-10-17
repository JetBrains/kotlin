// RUN_PIPELINE_TILL: CODEGEN
// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTIC_ARGUMENTS

package foo

class C

@JsExport
class E

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; C")!>C<!>.ext1() {}

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; C?")!>C?<!>.ext2() {}

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; Unit")!>Unit<!>.ext3() {}

@JsExport
fun E.ok1() {}

@JsExport
fun String.ok2() {}

@JsExport
class Holder {
    fun <!NON_EXPORTABLE_TYPE("receiver; C")!>C<!>.memberExt() {}

    fun E.memberOk() {}
}
