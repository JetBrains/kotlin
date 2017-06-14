fun foo(vararg x: Short) {}

fun bar() {
    foo(*shortArrayOf<caret>(1))
}
