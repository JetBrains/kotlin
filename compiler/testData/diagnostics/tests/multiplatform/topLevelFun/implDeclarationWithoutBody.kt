// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header fun foo()

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

<!NON_MEMBER_FUNCTION_NO_BODY!>impl fun foo()<!>

<!NON_MEMBER_FUNCTION_NO_BODY, IMPLEMENTATION_WITHOUT_HEADER!>impl fun bar()<!>
