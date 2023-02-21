// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

fun box(): String {
    val arr = VArray(2) { if (it > 0) it else null }
    arr.iterator().next()
    return "OK"
}