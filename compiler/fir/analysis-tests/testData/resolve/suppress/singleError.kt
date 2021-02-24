fun foo(x: String) {}

@Suppress("INAPPLICABLE_CANDIDATE")
fun bar() {
    foo(10)
}
