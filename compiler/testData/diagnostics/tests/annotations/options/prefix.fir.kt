@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun foo(): Int {
    var x = 5
    @FunAnn ++x
    @ExprAnn ++x
    return x
}