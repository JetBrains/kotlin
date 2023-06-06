// !DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

expect fun f1(s: () -> String)
<!INCOMPATIBLE_MATCHING{JVM}!>expect inline fun f2(s: () -> String)<!>
expect inline fun f3(noinline s: () -> String)

expect fun f4(s: () -> String)
<!INCOMPATIBLE_MATCHING{JVM}!>expect inline fun f5(s: () -> String)<!>
expect inline fun f6(crossinline s: () -> String)

<!INCOMPATIBLE_MATCHING{JVM}!>expect fun f7(x: Any)<!>
<!INCOMPATIBLE_MATCHING{JVM}!>expect fun f8(vararg x: Any)<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual inline fun f1(noinline s: () -> String) {}
<!ACTUAL_WITHOUT_EXPECT!>actual inline fun f2(noinline s: () -> String) {}<!>
actual inline fun f3(s: () -> String) {}
actual inline fun f4(crossinline s: () -> String) {}
<!ACTUAL_WITHOUT_EXPECT!>actual inline fun f5(crossinline s: () -> String) {}<!>
actual inline fun f6(s: () -> String) {}
<!ACTUAL_WITHOUT_EXPECT!>actual fun f7(vararg x: Any) {}<!>
<!ACTUAL_WITHOUT_EXPECT!>actual fun f8(x: Any) {}<!>
