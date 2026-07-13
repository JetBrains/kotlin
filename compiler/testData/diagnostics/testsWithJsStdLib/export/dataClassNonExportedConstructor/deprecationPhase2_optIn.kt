// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@file:JsExport

@ConsistentCopyVisibility
data class Data1 @JsExport.Ignore constructor(val x: Int)

@JsExport.Ignore
class NotExportedClass

@ConsistentCopyVisibility
data class Data2 @JsExport.Ignore constructor(
    <!NON_EXPORTABLE_TYPE!>val x: NotExportedClass<!>
)

@ConsistentCopyVisibility
data class Data3 @JsExport.Ignore constructor(
    @JsExport.Ignore val x: NotExportedClass
)
