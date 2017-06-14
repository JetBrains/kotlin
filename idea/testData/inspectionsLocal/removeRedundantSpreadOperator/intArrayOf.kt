fun foo(vararg x: Int) {}

fun bar() {
    foo(*intArrayOf<caret>(1))
}
