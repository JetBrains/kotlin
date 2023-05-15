// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun foo(b: Boolean, x: Int): Int {
    val vArray = if (b) {
        val y = x + 1;
        VArray(2) { Point(it + y, it + y + 1) }
    } else {
        val z = x * 2;
        VArray(2) { Point(it + z, it + z + 1) }
    }
    return vArray[1].y
}

fun box(): String {

    if (foo(false, 2) != 6) return "Fail 1"
    if (foo(true, 1) != 4) return "Fail 2"

    return "OK"
}