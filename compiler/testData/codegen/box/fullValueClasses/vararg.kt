// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// WITH_STDLIB

value class Point(val x: Int, val y: Int)
value class Point1(val x: Int)

fun f(vararg points: Point) = points.asList().toString()
fun f(vararg points: Point1) = points.asList().toString()

fun box(): String {
    require(f(Point(1, 2), Point(3, 4)) == "[Point(x=1, y=2), Point(x=3, y=4)]")
    val points = arrayOf(Point(5, 6), Point(7, 8))
    require(f(*points) == "[Point(x=5, y=6), Point(x=7, y=8)]")
    require(f(Point1(1), Point1(2)) == "[Point1(x=1), Point1(x=2)]")
    return "OK"
}
