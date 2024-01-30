// MODULE: m1-common
// FILE: common.kt

expect class A

// FILE: common2.kt
actual class <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>A<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

expect class <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>B<!>

// FILE: jvm2.kt
actual class <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>B<!>
