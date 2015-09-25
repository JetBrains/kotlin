@Target(AnnotationTarget.CLASS)
annotation class ClsAnn

@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

@Target(AnnotationTarget.EXPRESSION)
annotation class ExprAnn

fun bar(arg: () -> Int) = arg()

open class My

fun foo(arg: Int): My {
    bar @FunAnn { arg }
    bar @ExprAnn { arg }
    val x = @FunAnn fun() = arg
    // TODO: KT-9320: ClsAnn does not appear in bytecode
    return (@ClsAnn object: My() {})
}