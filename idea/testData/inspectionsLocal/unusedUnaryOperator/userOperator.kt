// PROBLEM: none
data class Point(val x: Int)

operator fun Point.plus(other: Point) = Point(this.x + other.x)

operator fun Point.unaryMinus() = Point(-x)

fun test() {
    val p = Point(1) + Point(2)
    <caret>-Point(3)
} 