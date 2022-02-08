@Target(AnnotationTarget.TYPE)
annotation class TypeAnnotation

fun x(): List<@TypeAnnotation In<caret>t> {
    TODO()
}