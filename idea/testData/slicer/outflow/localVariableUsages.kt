// FLOW: OUT
fun foo(n: Int) {
}

fun test() {
    val <caret>x = 1

    val y = x

    val z: Int
    z = x

    foo(x)
}