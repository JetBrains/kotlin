// RUN_PIPELINE_TILL: FRONTEND

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun foo(): Int {
    var a: Int
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!><!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>@ExprAnn a<!> = 1<!>
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@ExprAnn a <!UNRESOLVED_REFERENCE!>+=<!> 1<!>
    return a
}
