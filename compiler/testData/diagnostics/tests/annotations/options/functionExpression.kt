@Target(AnnotationTarget.EXPRESSION)
annotation class ExprAnn

@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

fun foo(): Int {
    val x = @ExprAnn fun() = 1
    val y = @FunAnn fun() = 2
    return x() + y()    
}