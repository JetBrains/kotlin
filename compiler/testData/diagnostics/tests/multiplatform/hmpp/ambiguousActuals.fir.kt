// MODULE: common
// TARGET_PLATFORM: Common
<!AMBIGUOUS_ACTUALS{JVM}!>expect fun foo()<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual <!CONFLICTING_OVERLOADS!>fun foo()<!> {}

// MODULE: main()()(common, intermediate)
actual fun foo() {}
