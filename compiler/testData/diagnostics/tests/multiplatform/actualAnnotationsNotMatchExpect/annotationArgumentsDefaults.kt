// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>(val p: String = "")
<!CONFLICTING_OVERLOADS!>@Ann("")
expect fun explicitDefaultArgument()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
// No special handling for this case
@Ann
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>explicitDefaultArgument<!>() {}
