// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun box(): String {

    val vArray2D = VArray(2) { i -> VArray(2) { j -> Point(i + j, i + j + 1) } }

    if (vArray2D[1][1].y != 3) return "Fail"

    return "OK"
}