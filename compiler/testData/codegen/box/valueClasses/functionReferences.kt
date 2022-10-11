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
    
    return "OK"
}
