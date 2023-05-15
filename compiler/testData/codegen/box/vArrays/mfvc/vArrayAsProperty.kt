// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)
class A(val arr: VArray<Point>)

fun box(): String {

    val a = A(VArray(2) { Point(it, it + 1) })

    if (a.arr[1].y != 2) return "Fail"

    return "OK"
}