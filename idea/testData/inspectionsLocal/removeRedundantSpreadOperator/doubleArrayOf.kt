fun foo(vararg x: Double) {}

fun bar() {
    foo(*doubleArrayOf<caret>(1.0))
}
