fun foo(vararg x: Long) {}

fun bar() {
    foo(*longArrayOf<caret>(1L))
}
