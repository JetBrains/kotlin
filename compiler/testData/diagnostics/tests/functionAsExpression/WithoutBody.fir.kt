// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

annotation class ann
val bas = <!NON_MEMBER_FUNCTION_NO_BODY!>fun ()<!>

fun bar(a: Any) = <!NON_MEMBER_FUNCTION_NO_BODY!>fun ()<!>

fun outer() {
    bar(<!NON_MEMBER_FUNCTION_NO_BODY!>fun ()<!>)
    bar(l@ <!NON_MEMBER_FUNCTION_NO_BODY!>fun ()<!>)
    bar(@ann <!NON_MEMBER_FUNCTION_NO_BODY!>fun ()<!>)
}