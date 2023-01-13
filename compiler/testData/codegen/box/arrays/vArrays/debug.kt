// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER


fun box(): String {

    val x = VArray<Int>(1) { 42 }
    val y = x[0]

    return "OK1"
}

