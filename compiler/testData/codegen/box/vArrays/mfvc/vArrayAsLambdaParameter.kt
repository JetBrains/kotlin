// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun foo(arr: VArray<Point>, func: (VArray<Point>) -> Int) = func(arr)

fun box(): String {

    if (foo(VArray(2) { Point(it, it + 1) }) { it[1].y } != 2) return "Fail"

    return "OK"
}