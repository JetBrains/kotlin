// MODULE: m1-common
// FILE: common.kt
annotation class Ann

@Ann
expect fun foo(p: Array<Int> = arrayOf())

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("fun foo(p: Array<Int> = ...): Unit defined in root package in file common.kt; fun foo(p: Array<Int>): Unit defined in root package in file jvm.kt; Annotation `@Ann` is missing on actual declaration")!>foo<!>(p: Array<Int>) {}
