// ENABLE_JVM_IR_INLINER
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR


inline fun <T> foo(p: IntArray) = p.iterator().next().toString()

fun box(): String {
    return "OK1"
}