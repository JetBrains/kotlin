// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    @ann ""<caret>!!
}

@Target(AnnotationTarget.EXPRESSION)
annotation class ann