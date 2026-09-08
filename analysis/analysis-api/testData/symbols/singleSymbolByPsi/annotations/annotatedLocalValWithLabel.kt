annotation class MyAnnotation

fun foo() {
    @MyAnnotation
    label@ val a<caret>a = 55
}
