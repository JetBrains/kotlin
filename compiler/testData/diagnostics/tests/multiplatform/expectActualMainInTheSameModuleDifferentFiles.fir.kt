// MODULE: m1-common
// FILE: common.kt
expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>()

// FILE: common2.kt
<!CONFLICTING_OVERLOADS!>actual fun <!ACTUAL_WITHOUT_EXPECT!>main<!>()<!> {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>()

// FILE: jvm2.kt
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>() {}
