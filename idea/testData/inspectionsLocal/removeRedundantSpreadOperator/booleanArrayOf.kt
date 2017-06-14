fun foo(vararg x: Boolean) {}

fun bar() {
    foo(*booleanArrayOf<caret>(true, true))
}
