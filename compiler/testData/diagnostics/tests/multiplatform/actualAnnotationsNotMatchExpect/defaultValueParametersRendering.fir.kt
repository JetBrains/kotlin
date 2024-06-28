// MODULE: m1-common
// FILE: common.kt
annotation class Ann

@Ann
expect fun foo(p: Array<Int> = arrayOf())

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun foo(p: Array<Int>) {}<!>
