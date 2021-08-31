@Target(AnnotationTarget.TYPE)
annotation class Anno1
@Target(AnnotationTarget.TYPE)
annotation class Anno2
@Target(AnnotationTarget.TYPE)
annotation class Anno3
@Target(AnnotationTarget.TYPE)
annotation class Anno4

interface I

class X : @Anno1 I {
    fun f(arg: @Anno2 I): @Anno3 I = arg
    val x: @Anno4 I = this
}
