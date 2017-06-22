// PROBLEM: none

interface Point {
    val x: Int
    val y: Int
}

class PointImpl(override val x: Int, override val y: Int) : Point

fun foo() {
    val p: <caret>Point = PointImpl(1, 2)
}