// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

value class Point(val x: Int, val y: Int)
value class Segment(val start: Point, val end: Point)

fun Point.render(): String = "P:$x:$y"
fun Segment.render(): String = "S:${start.x}:${start.y}:${end.x}:${end.y}"

fun box(): String {
    val pointReceiver: () -> String = Point(1, 2)::render
    val segmentReceiver: () -> String = Segment(Point(1, 2), Point(3, 4))::render

    val first = pointReceiver()
    if (first != "P:1:2") return "FAIL point receiver"

    val second = segmentReceiver()
    if (second != "S:1:2:3:4") return "FAIL segment receiver"

    return "OK"
}
