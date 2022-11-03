// FIR_DISABLE_LAZY_RESOLVE_CHECKS
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun foo(): Int {
    var a: Int
    @ExprAnn a = 1
    @ExprAnn a += 1
    return a
}
