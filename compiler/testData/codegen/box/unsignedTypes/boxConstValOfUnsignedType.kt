// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

const val maxUByte: UByte = 0xFFu

fun custom(a: Any): String {
    return "Custom: $a, isUByte: ${a is UByte}"
}

fun box(): String {
    val result = custom(maxUByte)
    if (result != "Custom: 255, isUByte: true") return "Fail: $result"

    return "OK"
}