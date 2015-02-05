// !DIAGNOSTICS: -UNUSED_PARAMETER

annotation class ann
val bas = <!NON_MEMBER_FUNCTION_NO_BODY!>fun ()<!>

fun bar(a: Any) = <!NON_MEMBER_FUNCTION_NO_BODY!>fun name()<!>

fun outer() {
    bar(<!NON_MEMBER_FUNCTION_NO_BODY!>fun ()<!>)
    bar(<!NON_MEMBER_FUNCTION_NO_BODY!>fun name()<!>)
    bar(<!NON_MEMBER_FUNCTION_NO_BODY!>[ann] fun name()<!>)
}