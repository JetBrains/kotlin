// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

annotation class MyAnnotation

fun foo() {
    @MyAnnotation
    label@ class B<caret>ar {}
}
