// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-72941

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun foo(y: Int) {
    var x = 1
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>@Ann x<!> += 2

    @Ann
    x += 2
}
