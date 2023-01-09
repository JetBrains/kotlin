// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

inline fun <reified T> getFirst(p: VArray<T>) = p[0]

inline fun <reified T> setFirst(p: VArray<T>, value: T) {
    p[0] = value
}

fun box(): String {
    val vArray = VArray(2) { 42 }
    if (getFirst(vArray) != 42) return "Fail 1"
    setFirst(vArray, 24)
    if (getFirst(vArray) != 24) return "Fail 2"
    return "OK"
}
