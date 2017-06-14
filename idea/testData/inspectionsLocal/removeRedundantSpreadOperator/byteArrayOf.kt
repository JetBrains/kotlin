fun foo(vararg x: Byte) {}

fun bar() {
    foo(*byteArrayOf<caret>(1))
}
