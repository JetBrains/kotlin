// MODULE: m1-common
// FILE: common.kt

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class A<!>

// FILE: common2.kt
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class A<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class B<!>

// FILE: jvm2.kt
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class B<!>
