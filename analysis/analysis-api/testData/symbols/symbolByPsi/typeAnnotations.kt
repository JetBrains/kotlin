// DO_NOT_CHECK_SYMBOL_RESTORE_K1
@Target(AnnotationTarget.TYPE)
annotation class Anno1
@Target(AnnotationTarget.TYPE)
annotation class Anno2
@Target(AnnotationTarget.TYPE)
annotation class Anno3
@Target(AnnotationTarget.TYPE)
annotation class Anno4
@Target(AnnotationTarget.TYPE)
annotation class Anno5(val s: String)

interface I

class X : @Anno1 I {
    fun f(arg: @Anno2 I): @Anno3 I = arg
    val x: @Anno4 I = this
}

fun <T> T.foo(): List<@Anno5("1") T>? = null
fun <T> T.foo2(): List<List<@Anno5("1") T>>? = null
