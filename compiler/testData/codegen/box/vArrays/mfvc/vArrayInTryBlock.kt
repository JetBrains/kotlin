// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun foo(x: Int) = try {
    1 / x
    VArray(2) { Point(it + x, it + x + 1) }
} catch (t: Throwable) {
    VArray(2) { Point(it + x + 2, it + x + 3) }
}

fun box(): String {

    if (foo(0)[1].y != 4) return "Fail 1"
    if (foo(1)[1].y != 3) return "Fail 2"

    return "OK"
}