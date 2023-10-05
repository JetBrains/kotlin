// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class Ann

<!INCOMPATIBLE_MATCHING{JVM}!>expect fun <T : @Ann Any> foo()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <T : <!UNRESOLVED_REFERENCE!>Unresolved<!>> foo() {}
