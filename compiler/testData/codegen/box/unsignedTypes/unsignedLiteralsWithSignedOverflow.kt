// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

fun box(): String {
    val u1: UByte = 255u
    if (u1.toByte().toInt() != -1) return "fail"

    return "OK"
}
