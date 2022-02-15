// MODULE: m1-common
// FILE: common.kt

expect fun <!AMBIGUOUS_ACTUALS{JVM}, AMBIGUOUS_ACTUALS{JS}!>foo<!>()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}

// MODULE: m3-js()()(m1-common)
// FILE: js.kt

<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
