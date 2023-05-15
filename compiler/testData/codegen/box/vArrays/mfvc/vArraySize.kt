// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun box(): String {

    if (VArray(2) { Point(-1, 1) }.size != 2) return "Fail 1"

    val vArray2D = VArray(2) { VArray(3) { Point(-1, 1) } }

    if (vArray2D.size != 2) return "Fail 2"
    if (vArray2D[1].size != 3) return "Fail 3"

    return "OK"
}