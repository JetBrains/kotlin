// !LANGUAGE: +ExpectedTypeFromCast

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class bar

fun <T> foo(): T = TODO()

fun <V> id(value: V) = value

val par1 = (foo()) as String
val par2 = ((foo())) as String

val par3 = (<!REDUNDANT_LABEL_WARNING!>dd@<!> (foo())) as String

val par4 = ( @bar() (foo())) as String

object X {
    fun <T> foo(): T = TODO()
}

val par5 = ( @bar() X.foo()) as String