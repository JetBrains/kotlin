// LANGUAGE: +LocalTypeAliases

@Target(AnnotationTarget.TYPEALIAS)
annotation class MyAnnotation

fun foo() {
    @MyAnnotation
    label@ typealias MyA<caret>lias = Int
}
