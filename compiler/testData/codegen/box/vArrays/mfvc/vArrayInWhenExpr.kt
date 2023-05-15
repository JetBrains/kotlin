// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun foo(c: Int, x: Int): Int {
    val vArray = when (c) {
        0 -> {
            val a = x + 1
            VArray(2) { Point(it + a, it + a + 1) }
        }
        1 -> {
            val b = x + 2
            VArray(2) { Point(it + b, it + b + 1) }
        }
        else -> {
            val c = x + 3
            VArray(2) { Point(it + c, it + c + 1) }
        }
    }
    return vArray[1].y
}

fun box(): String {

    if (foo(0, 1) != 4) return "Fail 1"
    if (foo(1, 2) != 6) return "Fail 2"
    if (foo(2, 3) != 8) return "Fail 3"

    return "OK"
}