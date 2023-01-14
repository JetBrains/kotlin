// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

fun isDouble3D(p: Any?) = p is VArray<VArray<VArray<Double>>>

fun box(): String {


    val double3dVArray = VArray(1) { VArray(1) { VArray(1) { 0.0 } } }

    if (!isDouble3D(double3dVArray)) return "Fail"

    return "OK"
}

