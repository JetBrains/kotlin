// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// LANGUAGE: +ValueClasses

fun box(): String {
    val x = intArrayOf(1, 2)
    x.size
    return "OK"
}