// MODULE: common
// TARGET_PLATFORM: Common
expect fun <!AMBIGUOUS_ACTUALS{JVM}, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>foo<!>()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
<!CONFLICTING_OVERLOADS{JVM}!>actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>foo<!>()<!> {}

// MODULE: main()()(common, intermediate)
<!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
