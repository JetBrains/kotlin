// Result type can be annotated
target(AnnotationTarget.TYPE)
annotation class My(val x: Int)

fun foo(): @My(42) Int = 24