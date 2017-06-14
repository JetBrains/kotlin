fun foo(vararg x: String) {}

fun bar() {
    foo(*emptyArray<caret><String>())
}
