// MODULE: common
// TARGET_PLATFORM: Common
<!AMBIGUOUS_ACTUALS{JVM}, NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun foo()<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual fun foo() {}

// MODULE: main()()(common, intermediate)
actual fun foo() {}
