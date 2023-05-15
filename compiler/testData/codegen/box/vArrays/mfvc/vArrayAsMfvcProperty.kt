// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

@JvmInline
value class Poly(val s: Point, val points: VArray<Point>)

fun box(): String {

    val poly = Poly(s = Point(1, 2), points = VArray(3) { Point(it, it + 1) })

    if (poly.points[2].y != 3) return "Fail"

    return "OK"
}