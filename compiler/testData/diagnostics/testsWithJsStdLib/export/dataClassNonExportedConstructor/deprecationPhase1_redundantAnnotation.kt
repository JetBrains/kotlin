// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility

<!REDUNDANT_ANNOTATION!>@kotlin.ConsistentCopyVisibility<!>
data class DataA @JsExport.Ignore constructor(val x: Int)

<!REDUNDANT_ANNOTATION!>@kotlin.ExposedCopyVisibility<!>
data class DataB @JsExport.Ignore constructor(val x: Int)
