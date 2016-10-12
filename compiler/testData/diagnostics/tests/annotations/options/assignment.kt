@Target(AnnotationTarget.EXPRESSION)
annotation class ExprAnn

fun foo(): Int {
    var a: Int
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@ExprAnn a<!> = 1
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@ExprAnn a<!> += 1
    return a
}
