annotation class MyAnnotation

fun foo() {
    @MyAnnotation
    label@ fun b<caret>ar() {}
}
