// MODULE: m1-common
// FILE: common.kt
annotation class Ann(val p: Double)

@Ann(0.3)
expect fun floatNumbersComparison()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@Ann(0.1 + 0.1 + 0.1)
actual fun floatNumbersComparison() {}<!>
