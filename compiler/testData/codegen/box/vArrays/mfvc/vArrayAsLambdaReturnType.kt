// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

fun foo(x: Int, func: (Int) -> VArray<Point>) = func(x)[1].y

fun box(): String {

    if (foo(1, { x -> VArray(2) { i -> Point(i + x, i + x + 1) } }) != 3) return "Fail"

    return "OK"
}