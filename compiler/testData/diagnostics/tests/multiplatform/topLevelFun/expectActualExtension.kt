// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
<!CONFLICTING_OVERLOADS!>expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!>Int<!>.test1()<!>

<!CONFLICTING_OVERLOADS!>expect fun test2<!NO_ACTUAL_FOR_EXPECT{JVM}!>(a: Int)<!><!>

<!CONFLICTING_OVERLOADS!>expect fun test3(x: (String) -> Unit)<!>

<!CONFLICTING_OVERLOADS!>expect fun test4(x: String.() -> Unit)<!>

<!CONFLICTING_OVERLOADS!>expect fun ((String) -> Unit).test5()<!>

<!CONFLICTING_OVERLOADS!>expect fun (String.() -> Unit).test6()<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual fun test1<!ACTUAL_WITHOUT_EXPECT!>(x: Int)<!>{}

actual fun <!ACTUAL_WITHOUT_EXPECT!>Int<!>.test2(){}

actual fun test3(x: String.() -> Unit){}

actual fun test4(x: (String) -> Unit){}

actual fun (String.() -> Unit).test5(){}

actual fun ((String) -> Unit).test6(){}
