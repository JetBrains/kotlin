// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

inline fun <reified T> getFirst(p: VArray<T>) = p[0]

inline fun <reified T> setFirst(p: VArray<T>, value: T) {
    p[0] = value
}

fun box(): String {
    val vArrayInt = VArray(2) { 42 }
    if (getFirst(vArrayInt) != 42) return "Fail 1.1"
    setFirst(vArrayInt, 24)
    if (getFirst(vArrayInt) != 24) return "Fail 2.1"

    val vArrayString = VArray(2) { "a" }
    if (getFirst(vArrayString) != "a") return "Fail 1.1"
    setFirst(vArrayString, "b")
    if (getFirst(vArrayString) != "b") return "Fail 2.1"

    return "OK"
}
