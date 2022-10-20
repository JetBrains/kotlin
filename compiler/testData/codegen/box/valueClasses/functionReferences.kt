// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double) {
    fun f(z: Double) = x + y + z
}

fun g(point: DPoint, z: Double) = point.f(z)

class A(val point: DPoint) {
    fun f(otherDPoint: DPoint, z: Double) = point.f(z) * otherDPoint.f(z)
}

fun consume(point1: DPoint, point2: DPoint, f: (DPoint, DPoint) -> DPoint) = f(point1, point2)

inline fun consumeInline(point1: DPoint, point2: DPoint, f: (DPoint, DPoint) -> DPoint) = f(point1, point2)

operator fun DPoint.plus(other: DPoint) = DPoint(this.x + other.x, this.y + other.y)

fun makeDPoint(x: Double, y: Double, maker: (Double, Double) -> DPoint) = maker(x, y)

inline fun makeDPointInline(x: Double, y: Double, maker: (Double, Double) -> DPoint) = maker(x, y)

fun box(): String {
    val dPoint = DPoint(1.0, 2.0)
    val a = A(dPoint)
    
    require((::DPoint)(1.0, 2.0) == dPoint)
    require((dPoint::f)(3.0) == 6.0)
    require((::g)(dPoint, 3.0) == 6.0)
    require((a::f)(dPoint, 3.0) == 36.0)
    
    require((::DPoint)(1.0, DPoint(1.0, 2.0).y) == dPoint)
    require((dPoint::f)(DPoint(1.0, 3.0).y) == 6.0)
    require((::g)(dPoint, DPoint(1.0, 3.0).y) == 6.0)
    require((a::f)(dPoint, DPoint(1.0, 3.0).y) == 36.0)
    
    require(consume(DPoint(1.0, 2.0), DPoint(3.0, 4.0), DPoint::plus) == DPoint(4.0, 6.0))
    require(consumeInline(DPoint(1.0, 2.0), DPoint(3.0, 4.0), DPoint::plus) == DPoint(4.0, 6.0))
    
    require(makeDPoint(1.0, 2.0, ::DPoint) == DPoint(1.0, 2.0))
    require(makeDPointInline(1.0, 2.0, ::DPoint) == DPoint(1.0, 2.0))
    
    require(::DPoint == ::DPoint)
    require(dPoint::f == dPoint::f)
    require(::g == ::g)
    require(a::f == a::f)
    require(DPoint::plus == DPoint::plus)
    
    return "OK"
}
