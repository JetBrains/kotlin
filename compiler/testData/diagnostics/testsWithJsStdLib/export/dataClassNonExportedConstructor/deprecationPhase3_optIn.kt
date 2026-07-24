// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, +DataClassCopyRespectsConstructorVisibility
@JsExport
<!REDUNDANT_ANNOTATION!>@ConsistentCopyVisibility<!>
data class Data1 @JsExport.Ignore constructor(val x: Int)

class NotExportedClass

@JsExport
<!REDUNDANT_ANNOTATION!>@ConsistentCopyVisibility<!>
data class Data2 @JsExport.Ignore constructor(
    <!NON_EXPORTABLE_TYPE!>val x: NotExportedClass<!>
)

@JsExport
<!REDUNDANT_ANNOTATION!>@ConsistentCopyVisibility<!>
data class Data3 @JsExport.Ignore constructor(
    @JsExport.Ignore val x: NotExportedClass
)
