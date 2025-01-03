// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect<!> fun Int.test1()

<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect<!> fun test2(a: Int)

expect fun test3(x: (String) -> Unit)

expect fun test4(x: String.() -> Unit)

expect fun ((String) -> Unit).test5()

expect fun (String.() -> Unit).test6()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <!ACTUAL_WITHOUT_EXPECT!>test1<!>(x: Int){}

actual fun Int.<!ACTUAL_WITHOUT_EXPECT!>test2<!>(){}

actual fun test3(x: String.() -> Unit){}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun test4(x: (String) -> Unit){}

actual fun (String.() -> Unit).test5(){}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun ((String) -> Unit).test6(){}
