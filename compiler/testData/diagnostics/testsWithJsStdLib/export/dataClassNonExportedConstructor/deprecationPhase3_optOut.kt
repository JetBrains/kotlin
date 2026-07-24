// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, +DataClassCopyRespectsConstructorVisibility
@file:JsExport

@ExposedCopyVisibility
data class Data1 @JsExport.Ignore constructor(val x: Int)

@JsExport.Ignore
class NotExportedClass

@ExposedCopyVisibility
data class Data2 @JsExport.Ignore constructor(
    <!NON_EXPORTABLE_TYPE, NON_EXPORTABLE_TYPE_IN_SYNTHETIC_COPY_FUNCTION_WITH_EXPOSED_COPY_VISIBILITY!>val x: NotExportedClass<!>
)

@ExposedCopyVisibility
data class Data3 @JsExport.Ignore constructor(
    <!NON_EXPORTABLE_TYPE_IN_SYNTHETIC_COPY_FUNCTION_WITH_EXPOSED_COPY_VISIBILITY!>@JsExport.Ignore val x: NotExportedClass<!>
)
