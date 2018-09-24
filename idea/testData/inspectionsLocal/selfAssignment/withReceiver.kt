// PROBLEM: none

class Point(var x: Int) {
    fun copyFrom(other: Point) {
        x = <caret>other.x
    }
}