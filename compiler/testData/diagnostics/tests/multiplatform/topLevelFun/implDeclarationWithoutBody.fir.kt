// MODULE: m1-common
// FILE: common.kt

expect fun foo()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual <!NON_MEMBER_FUNCTION_NO_BODY!>fun foo()<!>

actual <!NON_MEMBER_FUNCTION_NO_BODY!>fun <!ACTUAL_WITHOUT_EXPECT!>bar<!>()<!>
