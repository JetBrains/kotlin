class Point(val x: Int, val y: Int)

fun foo() {
    val p: <caret>Point = Point(1, 2)
}