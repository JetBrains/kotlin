@Target(AnnotationTarget.EXPRESSION)
annotation class ann

fun foo(): Int = <caret>@ann 1