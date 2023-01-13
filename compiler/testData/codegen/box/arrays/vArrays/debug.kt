// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER


fun box(): String {

    val x : Any = VArray<Int>(1) { 42 }
    val y = x is VArray<Int>
    return y.toString()

    return "OK1"
}

