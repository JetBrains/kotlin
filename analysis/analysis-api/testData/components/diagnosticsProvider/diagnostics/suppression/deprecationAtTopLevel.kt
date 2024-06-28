@Deprecated(message = "")
fun foo() {
}

@Suppress("DEPRECATION")
fun bar() {
    foo()
}
