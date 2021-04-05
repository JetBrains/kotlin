fun foo(x: String) {}

@Suppress("ARGUMENT_TYPE_MISMATCH")
fun bar() {
    foo(10)
}
