// MODULE: m1-common
// FILE: common.kt

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class A<!>
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class A<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class B<!>
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class B<!>
