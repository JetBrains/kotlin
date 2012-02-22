class Point(val x: Int, val y: Int) {
    fun toString() = "($x,$y)"

    fun distanceTo(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        return Math.sqrt((dx*dx + dy*dy).toDouble())
    }
}

fun main(args: Array<String>) {
    var p = Point(0, 0)
    var q = Point(1, 1)
    p.distanceTo(q)


}