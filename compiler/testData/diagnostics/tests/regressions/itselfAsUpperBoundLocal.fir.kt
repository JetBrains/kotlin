// !WITH_NEW_INFERENCE
fun bar() {
    fun <T: T?> foo() {}
    foo()
}
