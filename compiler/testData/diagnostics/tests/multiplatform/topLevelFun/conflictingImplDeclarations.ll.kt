// MODULE: m1-common
// FILE: common.kt

expect fun foo()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
actual <!CONFLICTING_OVERLOADS!>fun foo()<!> {}

// MODULE: m3-js()()(m1-common)
// FILE: js.kt

actual <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
actual <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
