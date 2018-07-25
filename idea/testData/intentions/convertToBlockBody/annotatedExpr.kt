@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ann

fun foo(): Int = <caret>@ann 1