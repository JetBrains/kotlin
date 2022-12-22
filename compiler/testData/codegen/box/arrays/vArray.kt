// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

fun box(): String {
    if (VArray(2) { "b" }[1] != "b") return "Fail 1"
    if (vArrayOfNulls<Int>(2)[1] != null) return "Fail 2"
    return "OK"
}
