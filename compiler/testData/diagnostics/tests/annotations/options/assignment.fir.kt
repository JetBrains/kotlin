// RUN_PIPELINE_TILL: FRONTEND

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun foo(): Int {
    var a: Int
    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>@ExprAnn a<!> = 1
    @ExprAnn a <!UNRESOLVED_REFERENCE!>+=<!> 1
    return a
}
