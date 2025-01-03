// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>(val p: Double)

<!CONFLICTING_OVERLOADS!>@Ann(0.3)
expect fun floatNumbersComparison()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann(0.1 + 0.1 + 0.1)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>floatNumbersComparison<!>() {}
