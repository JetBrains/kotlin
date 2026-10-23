// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTIC_ARGUMENTS

package foo

class C {
    companion object
}

@JsExport
class E {
    companion object
}

interface NI {
    companion object
}

@JsExport
interface EI {
    companion object
}

external class ExtC {
    companion object
}

external interface ExtI {
    companion object
}

// The receiver of a companion extension is not passed to the exported function, so it is not checked
@JsExport
companion fun C.companionExt() {}

@JsExport
companion fun E.companionOk(): String = "OK"

@JsExport
companion fun NI.companionExtOnNonExportedInterface() {}

@JsExport
companion fun EI.companionExtOnExportedInterface() {}

@JsExport
companion fun ExtC.companionExtOnExternalClass() {}

@JsExport
companion fun ExtI.companionExtOnExternalInterface() {}

@JsExport
companion fun E.companionExtWithParameter(<!NON_EXPORTABLE_TYPE("parameter; C")!>x: C<!>) {}

<!NON_EXPORTABLE_TYPE("return; C")!>@JsExport
companion fun E.companionExtWithReturn(): C<!> = C()

// A regular extension with the same receiver is still checked
@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; C")!>C<!>.regularExt() {}

// A regular extension on a companion object type has a real receiver, so it is checked
@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; C.Companion")!>C.Companion<!>.extOnCompanionOfClass() {}

@JsExport
fun E.Companion.extOnCompanionOfExportedClass() {}

@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; NI.Companion")!>NI.Companion<!>.extOnCompanionOfInterface() {}

@JsExport
fun EI.Companion.extOnCompanionOfExportedInterface() {}

@JsExport
fun ExtC.Companion.extOnCompanionOfExternalClass() {}

@JsExport
fun ExtI.Companion.extOnCompanionOfExternalInterface() {}
