// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER


fun box(): String {

    val arr = IntArray(2)
    arr.iterator()

    return "OK"
}