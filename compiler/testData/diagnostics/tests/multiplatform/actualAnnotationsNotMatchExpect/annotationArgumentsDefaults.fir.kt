// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class Ann(val p: String = "")
@Ann("")
expect fun explicitDefaultArgument()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
// No special handling for this case
@Ann
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun explicitDefaultArgument() {}
