// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class Ann

expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!><T : @Ann Any><!> foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <!ACTUAL_WITHOUT_EXPECT!><T : <!UNRESOLVED_REFERENCE!>Unresolved<!>><!> foo() {}
