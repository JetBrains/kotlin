// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)


fun box(): String {

    val a = VArray(2) { Point(it, it + 1) }
    val b = a
    val c = b

    if (c[1].y != 2) return "Fail"

    return "OK"
}