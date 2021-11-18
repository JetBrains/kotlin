@Target(AnnotationTarget.TYPE)
annotation class TypeAnnotation

fun x(): @TypeAnnotation Li<caret>st<Int> {
    TODO()
}