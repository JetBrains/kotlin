// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// FIR_IDENTICAL

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun box(): String {
    var point = DPoint(1.0, 2.0)
    repeat(10) {
        point = DPoint(3.0, 4.0)
    }
    return if (point == DPoint(3.0, 4.0)) "OK" else point.toString()
}
