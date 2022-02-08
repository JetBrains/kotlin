// FIR_IDENTICAL

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

fun <@Anno T> foo() {}