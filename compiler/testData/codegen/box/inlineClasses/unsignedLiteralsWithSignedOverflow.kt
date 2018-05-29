// !LANGUAGE: +InlineClasses
// !SKIP_METADATA_VERSION_CHECK
// WITH_UNSIGNED

fun box(): String {
    val u1: UByte = 255u
    if (u1.toByte().toInt() != -1) return "fail"

    return "OK"
}
