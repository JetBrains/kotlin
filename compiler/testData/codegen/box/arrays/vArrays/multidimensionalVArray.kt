// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER


fun box(): String {
    val doubleTensor = VArray(2) { VArray(3) { VArray(4) { 0.1 } } }
    doubleTensor[1][2][3] = 0.5
    if (doubleTensor[0][0][0] != 0.1) return "Fail 1"
    if (doubleTensor[1][2][3] != 0.5) return "Fail 2"

    return "OK"
}