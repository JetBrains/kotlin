// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses
@JvmInline
value class Point(val x: Int, val y: Int)

@JvmInline
value class Poly(val points: VArray<Point>)

@JvmInline
value class PolyWrapper(val poly: Poly)

fun box(): String {

    val poly = Poly(VArray(3) { Point(it, it + 1) })

    if (poly.points[2].y != 3) return "Fail 1"

    val polyWrapper = PolyWrapper(poly)
    if (polyWrapper.poly.points[2].y != 3) return "Fail 2"

    return "OK"
}