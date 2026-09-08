annotation class MyAnnotation

fun foo() {
    @MyAnnotation
    label@ class B<caret>ar {}
}
