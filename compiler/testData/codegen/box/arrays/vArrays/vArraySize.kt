// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses
// ENABLE_JVM_IR_INLINER

fun box(): String {
    val doubleTensor = VArray(2) { VArray(3) { VArray(4) { 0.1 } } }
    if (doubleTensor.size != 2) return "Fail 1"
    if (doubleTensor[1].size != 3) return "Fail 2"
    if (doubleTensor[1][1].size != 4) return "Fail 3"

    return "OK"
}