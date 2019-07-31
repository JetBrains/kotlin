fun main() {
    <caret>callMe()
    foo()
    bar(1, 2)
}

fun foo() {
    val y = 2.0
}

fun bar(x: Int, y: Int) = x + y