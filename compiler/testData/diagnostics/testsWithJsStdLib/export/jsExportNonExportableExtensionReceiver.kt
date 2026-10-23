// RUN_PIPELINE_TILL: CODEGEN
// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTIC_ARGUMENTS

package foo

class C

@JsExport
class E

interface NI

@JsExport
interface EI

external class ExtC

external interface ExtI

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; C")!>C<!>.ext1() {}

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; C?")!>C?<!>.ext2() {}

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; Unit")!>Unit<!>.ext3() {}

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; NI")!>NI<!>.ext4() {}

@JsExport
fun E.ok1() {}

@JsExport
fun String.ok2() {}

@JsExport
fun EI.ok3() {}

@JsExport
fun EI?.ok4() {}

@JsExport
fun ExtC.ok5() {}

@JsExport
fun ExtI.ok6() {}

@JsExport
fun ExtI?.ok7() {}

@JsExport
class Holder {
    fun <!NON_EXPORTABLE_TYPE("receiver; C")!>C<!>.memberExt() {}

    fun <!NON_EXPORTABLE_TYPE("receiver; NI")!>NI<!>.memberExtOnNonExportedInterface() {}

    fun E.memberOk() {}

    fun EI.memberOkOnExportedInterface() {}

    fun ExtC.memberOkOnExternalClass() {}

    fun ExtI.memberOkOnExternalInterface() {}
}
