fun foo(vararg x: Float) {}

fun bar() {
    foo(*floatArrayOf<caret>(1.0f))
}
