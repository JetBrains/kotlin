// MODULE: m1-common
// FILE: common.kt

<!INCOMPATIBLE_MATCHING{JVM}!>inline expect fun inlineFun()<!>
expect fun nonInlineFun()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

<!ACTUAL_WITHOUT_EXPECT!>actual fun inlineFun() { }<!>
actual fun nonInlineFun() { }

// MODULE: m3-js()()(m1-common)
// FILE: js.kt

actual <!NOTHING_TO_INLINE!>inline<!> fun inlineFun() { }
actual <!NOTHING_TO_INLINE!>inline<!> fun nonInlineFun() { }
