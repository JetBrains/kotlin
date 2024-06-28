// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING


@JvmInline
value
class Point(val x: Double, val y: Double)

data class Line(val a: Point = Point(-300.0, -400.0), val b: Point) {
    constructor(x0: Float, y0: Float, x1: Float, y1: Float) : this(Point(x0.toDouble(), y0.toDouble()), Point(x1.toDouble(), y1.toDouble()))
    constructor(a: Point = Point(-100.0, -200.0), x1: Float, y1: Float) : this(a, Point(x1.toDouble(), y1.toDouble()))
}

fun box(): String {
    if (Line(Point(1.0, 2.0), Point(3.0, 4.0)) != Line(1.0f, 2.0f, 3.0f, 4.0f)) return Line(1.0f, 2.0f, 3.0f, 4.0f).toString()
    if (Line(Point(1.0, 2.0), Point(3.0, 4.0)) != Line(Point(1.0, 2.0), 3.0f, 4.0f)) return Line(Point(1.0, 2.0), 3.0f, 4.0f).toString()
    if (Line(Point(-300.0, -400.0), Point(3.0, 4.0)) != Line(b = Point(3.0, 4.0))) return Line(b = Point(3.0, 4.0)).toString()
    if (Line(Point(-100.0, -200.0), Point(5.0, 6.0)) != Line(x1 = 5.0f, y1 = 6.0f)) return Line(x1 = 5.0f, y1 = 6.0f).toString()
    return "OK"
}
