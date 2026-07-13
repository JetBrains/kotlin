// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@file:JsExport

data class DataA <!DATA_CLASS_COPY_JS_EXPORTABILITY_WILL_BE_CHANGED_ERROR!>@JsExport.Ignore constructor(val x: Int)<!> {
    @JsExport.Ignore
    fun copy() = DataA(1)
}

data class DataB <!DATA_CLASS_COPY_JS_EXPORTABILITY_WILL_BE_CHANGED_ERROR!>@JsExport.Ignore <!JS_NAME_CLASH!>constructor(val x: Int)<!><!> {
    <!JS_NAME_CLASH!>fun copy()<!> = DataB(1)
}


@JsExport.Ignore
class NotExportedClass

data class DataC <!DATA_CLASS_COPY_JS_EXPORTABILITY_WILL_BE_CHANGED_ERROR!>@JsExport.Ignore constructor(
    <!NON_EXPORTABLE_TYPE, NON_EXPORTABLE_TYPE_IN_SYNTHETIC_COPY_WITHOUT_CONSISTENT_VISIBILITY!>val x: NotExportedClass<!>
)<!>

data class DataD <!DATA_CLASS_COPY_JS_EXPORTABILITY_WILL_BE_CHANGED_ERROR!>@JsExport.Ignore constructor(
    <!NON_EXPORTABLE_TYPE_IN_SYNTHETIC_COPY_WITHOUT_CONSISTENT_VISIBILITY!>@JsExport.Ignore val x: NotExportedClass<!>
)<!>


fun local(a: DataA, b: DataB, c: DataC, d: DataD) {
    a.copy()
    b.copy()
    c.copy()
    d.copy()
}

