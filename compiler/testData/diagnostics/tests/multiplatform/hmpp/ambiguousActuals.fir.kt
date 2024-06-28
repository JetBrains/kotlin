// MODULE: common
// TARGET_PLATFORM: Common
<!AMBIGUOUS_ACTUALS{JVM}!>expect fun foo()<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}

// MODULE: main()()(common, intermediate)
actual fun foo() {}
