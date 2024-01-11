// MODULE: m1-common
// FILE: common.kt

<!AMBIGUOUS_ACTUALS{JVM}, AMBIGUOUS_ACTUALS{JS}, NO_ACTUAL_FOR_EXPECT{JVM}, NO_ACTUAL_FOR_EXPECT{JS}!>expect fun foo()<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}

// MODULE: m3-js()()(m1-common)
// FILE: js.kt

<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
