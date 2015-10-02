@Target(AnnotationTarget.EXPRESSION)
annotation class ExprAnn

fun foo(): Int {
    var a: Int
    @ExprAnn a = 1
    @ExprAnn a += 1
    return a
}