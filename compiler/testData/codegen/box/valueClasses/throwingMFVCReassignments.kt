// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

@JvmInline
value class DPoint(val x: Double, val y: Double)

class PointBox(var value: DPoint)

fun box(): String {
    var p = DPoint(1.0, 2.0)
    try {
        p = DPoint(3.0, error("Failure"))
    } catch (_: Exception) {
    }
    if (p != DPoint(1.0, 2.0)) {
        return "Partially reassigned variable"
    }
    
    val box = PointBox(p)

    try {
        box.value = DPoint(3.0, error("Failure"))
    } catch (_: Exception) {
    }
    
    if (box.value != DPoint(1.0, 2.0)) {
        return "Partially reassigned field"
    }
    
    return "OK"
}