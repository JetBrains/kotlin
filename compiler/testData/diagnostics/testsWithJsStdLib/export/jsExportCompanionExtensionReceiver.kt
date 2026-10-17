// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTIC_ARGUMENTS

package foo

class C

@JsExport
class E

// The receiver of a companion extension is not passed to the exported function, so it is not checked
@JsExport
companion fun C.companionExt() {}

@JsExport
companion fun E.companionOk(): String = "OK"

@JsExport
companion fun E.companionExtWithParameter(<!NON_EXPORTABLE_TYPE("parameter; C")!>x: C<!>) {}

<!NON_EXPORTABLE_TYPE("return; C")!>@JsExport
companion fun E.companionExtWithReturn(): C<!> = C()

// A regular extension with the same receiver is still checked
@JsExport
fun <!NON_EXPORTABLE_TYPE("receiver; C")!>C<!>.regularExt() {}
