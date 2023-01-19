// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: String, val y: String)

@JvmInline
value class IcPoint(val x: Point)

@JvmInline
value class IcIcPoint(val x : IcPoint)

fun foo(x: IcIcPoint) {}

fun box(): String {
    return "OK1"
}