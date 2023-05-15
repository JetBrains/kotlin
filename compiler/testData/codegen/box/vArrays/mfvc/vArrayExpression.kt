// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun box(): String {

    VArray(2) { Point(it, it + 1) }[0] = Point(10, 20)

    val a = VArray(2) { Point(it, it + 1) }[1].y

    if (a != 2) return "Fail"

    return "OK"
}