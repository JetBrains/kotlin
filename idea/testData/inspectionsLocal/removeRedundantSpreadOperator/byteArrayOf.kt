fun foo(vararg x: Byte) {}

fun bar() {
    foo(*<caret>byteArrayOf(1))
}
