// !LANGUAGE: +ExpectedTypeFromCast

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class bar

fun <T> foo(): T = TODO()

fun <V> id(value: V) = value

val par1 = (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()) as String
val par2 = ((<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>())) as String

val par3 = (dd@ (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>())) as String

val par4 = ( @bar() (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>())) as String

object X {
    fun <T> foo(): T = TODO()
}

val par5 = ( @bar() X.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()) as String
