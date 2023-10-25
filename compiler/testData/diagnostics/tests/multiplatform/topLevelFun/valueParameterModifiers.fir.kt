// !DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

expect fun f1(s: () -> String)
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect inline fun f2(s: () -> String)<!>
expect inline fun f3(noinline s: () -> String)

expect fun f4(s: () -> String)
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect inline fun f5(s: () -> String)<!>
expect inline fun f6(crossinline s: () -> String)

<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun f7(x: Any)<!>
<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect fun f8(vararg x: Any)<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual inline fun f1(noinline s: () -> String) {}
actual inline fun <!ACTUAL_WITHOUT_EXPECT!>f2<!>(noinline s: () -> String) {}
actual inline fun f3(s: () -> String) {}
actual inline fun f4(crossinline s: () -> String) {}
actual inline fun <!ACTUAL_WITHOUT_EXPECT!>f5<!>(crossinline s: () -> String) {}
actual inline fun f6(s: () -> String) {}
actual fun <!ACTUAL_WITHOUT_EXPECT!>f7<!>(vararg x: Any) {}
actual fun <!ACTUAL_WITHOUT_EXPECT!>f8<!>(x: Any) {}
