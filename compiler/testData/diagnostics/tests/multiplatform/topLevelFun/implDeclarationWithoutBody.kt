// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect fun foo()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

<!NON_MEMBER_FUNCTION_NO_BODY!>actual fun foo()<!>

<!NON_MEMBER_FUNCTION_NO_BODY!>actual fun <!ACTUAL_WITHOUT_EXPECT!>bar<!>()<!>
