// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun foo(): Int {
    var a: Int
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>@ExprAnn a<!> = 1
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>@ExprAnn a<!> += 1
    return a
}
