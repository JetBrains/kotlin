// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// LANGUAGE: +LocalTypeAliases

@Target(AnnotationTarget.TYPEALIAS)
annotation class MyAnnotation

fun foo() {
    @MyAnnotation
    label@ typealias MyA<caret>lias = Int
}
