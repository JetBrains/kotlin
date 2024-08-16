// MODULE: m1-common
// FILE: common.kt
expect <!CONFLICTING_OVERLOADS!>fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{METADATA}!>main<!>()<!>

// FILE: common2.kt
actual <!CONFLICTING_OVERLOADS!>fun <!ACTUAL_WITHOUT_EXPECT, ACTUAL_WITHOUT_EXPECT{METADATA}, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{METADATA}!>main<!>()<!> {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>()

// FILE: jvm2.kt
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>() {}
