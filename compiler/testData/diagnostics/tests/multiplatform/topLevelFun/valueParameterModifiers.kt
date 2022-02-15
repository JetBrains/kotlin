// !DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

expect fun f1(s: () -> String)
expect inline fun f2(s: () -> String)
expect inline fun f3(noinline s: () -> String)

expect fun f4(s: () -> String)
expect inline fun f5(s: () -> String)
expect inline fun f6(crossinline s: () -> String)

expect fun f7<!NO_ACTUAL_FOR_EXPECT{JVM}!>(x: Any)<!>
expect fun f8<!NO_ACTUAL_FOR_EXPECT{JVM}!>(vararg x: Any)<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual inline fun f1(noinline s: () -> String) {}
actual inline fun f2<!ACTUAL_WITHOUT_EXPECT!>(noinline s: () -> String)<!> {}
actual inline fun f3(s: () -> String) {}
actual inline fun f4(crossinline s: () -> String) {}
actual inline fun f5<!ACTUAL_WITHOUT_EXPECT!>(crossinline s: () -> String)<!> {}
actual inline fun f6(s: () -> String) {}
actual fun f7<!ACTUAL_WITHOUT_EXPECT!>(vararg x: Any)<!> {}
actual fun f8<!ACTUAL_WITHOUT_EXPECT!>(x: Any)<!> {}
