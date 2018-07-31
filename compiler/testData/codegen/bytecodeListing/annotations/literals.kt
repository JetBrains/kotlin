@Target(AnnotationTarget.CLASS)
annotation class ClsAnn

@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun bar(arg: () -> Int) = arg()

open class My

fun foo(arg: Int): My {
    bar @FunAnn { arg }
    bar @ExprAnn { arg }
    val x = @FunAnn fun() = arg
    return (@ClsAnn object: My() {})
}